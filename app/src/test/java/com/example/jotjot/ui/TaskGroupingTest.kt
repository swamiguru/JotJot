package com.example.jotjot.ui

import com.example.jotjot.data.Priority
import com.example.jotjot.data.Recurrence
import com.example.jotjot.data.Task
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import java.util.Calendar
import java.util.TimeZone
import org.junit.Test

/**
 * Validates TaskGrouping's Monday-based buckets and grouping.
 *
 * Reference "now" is Wed 2026-07-15 12:00 (noon):
 *   This week : Mon 2026-07-13 .. Sun 2026-07-19
 *   Next week : Mon 2026-07-20 .. Sun 2026-07-26
 * A date-only task uses millisecond 0; a timed task uses the sentinel millisecond 1.
 * Timezone is pinned to UTC (TaskGrouping uses the JVM default) for determinism.
 */
class TaskGroupingTest {

    private val tz = TimeZone.getTimeZone("UTC")
    private val originalTz = TimeZone.getDefault()

    @Before fun pinTimezone() = TimeZone.setDefault(tz)

    @After fun restoreTimezone() = TimeZone.setDefault(originalTz)

    /** [ms] = 0 for a date-only task, 1 for a task with a specific time-of-day. */
    private fun at(y: Int, mo: Int, d: Int, h: Int = 0, mi: Int = 0, ms: Int = 0): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(y, mo, d, h, mi, 0)
            set(Calendar.MILLISECOND, ms)
        }.timeInMillis

    private val now = at(2026, Calendar.JULY, 15, 12, 0) // Wednesday noon

    private fun cat(dueDate: Long?) = TaskGrouping.categorize(dueDate, now)

    // ---------- No date ----------

    @Test fun nullDueDate_isNoDate() {
        assertEquals(TaskCategory.NO_DATE, cat(null))
    }

    // ---------- Overdue ----------

    @Test fun dateOnly_yesterday_isOverdue() {
        assertEquals(TaskCategory.OVERDUE, cat(at(2026, Calendar.JULY, 14)))
    }

    @Test fun dateOnly_earlierThisWeekBeforeToday_isOverdue() {
        assertEquals(TaskCategory.OVERDUE, cat(at(2026, Calendar.JULY, 13)))
    }

    @Test fun timed_earlierToday_pastItsTime_isOverdue() {
        // 8am today with a specific time, now is noon -> its moment has passed
        assertEquals(TaskCategory.OVERDUE, cat(at(2026, Calendar.JULY, 15, 8, 0, ms = 1)))
    }

    @Test fun timed_yesterday_isOverdue() {
        assertEquals(TaskCategory.OVERDUE, cat(at(2026, Calendar.JULY, 14, 17, 0, ms = 1)))
    }

    // ---------- Today ----------

    @Test fun dateOnly_today_isToday_notOverdue() {
        // No specific time: stays under Today for the whole day
        assertEquals(TaskCategory.TODAY, cat(at(2026, Calendar.JULY, 15)))
    }

    @Test fun timed_laterToday_isToday() {
        assertEquals(TaskCategory.TODAY, cat(at(2026, Calendar.JULY, 15, 17, 0, ms = 1)))
    }

    // ---------- This week (tomorrow .. Sunday) ----------

    @Test fun dateOnly_tomorrow_isThisWeek() {
        assertEquals(TaskCategory.THIS_WEEK, cat(at(2026, Calendar.JULY, 16)))
    }

    @Test fun timed_tomorrow_isThisWeek() {
        assertEquals(TaskCategory.THIS_WEEK, cat(at(2026, Calendar.JULY, 16, 9, 0, ms = 1)))
    }

    @Test fun dateOnly_endOfThisWeekSunday_isThisWeek() {
        assertEquals(TaskCategory.THIS_WEEK, cat(at(2026, Calendar.JULY, 19)))
    }

    // ---------- Next week ----------

    @Test fun dateOnly_nextMonday_isNextWeek() {
        assertEquals(TaskCategory.NEXT_WEEK, cat(at(2026, Calendar.JULY, 20)))
    }

    @Test fun dateOnly_endOfNextWeekSunday_isNextWeek() {
        assertEquals(TaskCategory.NEXT_WEEK, cat(at(2026, Calendar.JULY, 26)))
    }

    // ---------- Someday ----------

    @Test fun dateOnly_weekAfterNext_isSomeday() {
        assertEquals(TaskCategory.SOMEDAY, cat(at(2026, Calendar.JULY, 27)))
    }

    @Test fun dateOnly_farFuture_isSomeday() {
        assertEquals(TaskCategory.SOMEDAY, cat(at(2026, Calendar.DECEMBER, 1)))
    }

    // ---------- groupActive ----------

    private fun task(id: Long, dueDate: Long?) = Task(
        id = id,
        title = "t$id",
        notes = null,
        isCompleted = false,
        createdAt = 0L,
        dueDate = dueDate,
        priority = Priority.MEDIUM,
        recurrence = Recurrence.NONE,
    )

    @Test fun groupActive_returnsNonEmptyBucketsInFixedOrder() {
        val tasks = listOf(
            task(6, null),                                          // No Date
            task(4, at(2026, Calendar.JULY, 20)),                  // Next Week
            task(1, at(2026, Calendar.JULY, 15, 8, 0, ms = 1)),    // Overdue (timed, past)
            task(5, at(2026, Calendar.JULY, 27)),                  // Someday
            task(2, at(2026, Calendar.JULY, 15)),                  // Today (date-only)
            task(3, at(2026, Calendar.JULY, 16)),                  // This Week
        )
        val groups = TaskGrouping.groupActive(tasks, now)
        assertEquals(
            listOf(
                TaskCategory.OVERDUE,
                TaskCategory.TODAY,
                TaskCategory.THIS_WEEK,
                TaskCategory.NEXT_WEEK,
                TaskCategory.SOMEDAY,
                TaskCategory.NO_DATE,
            ),
            groups.map { it.first },
        )
        assertEquals(listOf(1L), groups[0].second.map { it.id })
        assertEquals(listOf(2L), groups[1].second.map { it.id })
        assertEquals(listOf(3L), groups[2].second.map { it.id })
        assertEquals(listOf(4L), groups[3].second.map { it.id })
        assertEquals(listOf(5L), groups[4].second.map { it.id })
        assertEquals(listOf(6L), groups[5].second.map { it.id })
    }

    @Test fun groupActive_omitsEmptyBuckets() {
        val tasks = listOf(
            task(1, null),                          // No Date
            task(2, at(2026, Calendar.JULY, 15)),   // Today
        )
        val groups = TaskGrouping.groupActive(tasks, now)
        assertEquals(listOf(TaskCategory.TODAY, TaskCategory.NO_DATE), groups.map { it.first })
    }

    @Test fun groupActive_preservesInputOrderWithinBucket() {
        val tasks = listOf(
            task(10, at(2026, Calendar.JULY, 18)),
            task(11, at(2026, Calendar.JULY, 17)),
        )
        val groups = TaskGrouping.groupActive(tasks, now)
        assertEquals(1, groups.size)
        assertEquals(TaskCategory.THIS_WEEK, groups[0].first)
        assertEquals(listOf(10L, 11L), groups[0].second.map { it.id })
    }

    @Test fun groupActive_emptyInput_returnsEmpty() {
        assertEquals(emptyList<Pair<TaskCategory, List<Task>>>(), TaskGrouping.groupActive(emptyList(), now))
    }
}
