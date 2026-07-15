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
 * Reference week for the fixed "now" of Wed 2026-07-15:
 *   This week : Mon 2026-07-13 .. Sun 2026-07-19
 *   Next week : Mon 2026-07-20 .. Sun 2026-07-26
 * Timezone is pinned to UTC (TaskGrouping uses the JVM default) for determinism.
 */
class TaskGroupingTest {

    private val tz = TimeZone.getTimeZone("UTC")
    private val originalTz = TimeZone.getDefault()

    @Before fun pinTimezone() = TimeZone.setDefault(tz)

    @After fun restoreTimezone() = TimeZone.setDefault(originalTz)

    private fun at(y: Int, mo: Int, d: Int, h: Int = 9, mi: Int = 0): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(y, mo, d, h, mi, 0)
        }.timeInMillis

    private val now = at(2026, Calendar.JULY, 15, 12, 0) // Wednesday noon

    private fun cat(dueDate: Long?) = TaskGrouping.categorize(dueDate, now)

    // ---------- categorize ----------

    @Test fun nullDueDate_isSomeday() {
        assertEquals(TaskCategory.SOMEDAY, cat(null))
    }

    @Test fun yesterday_isOverdue() {
        assertEquals(TaskCategory.OVERDUE, cat(at(2026, Calendar.JULY, 14, 9, 0)))
    }

    @Test fun earlierThisWeekButBeforeToday_isOverdue() {
        // Monday of this week, before Wednesday "now"
        assertEquals(TaskCategory.OVERDUE, cat(at(2026, Calendar.JULY, 13, 9, 0)))
    }

    @Test fun earlierToday_isThisWeek_notOverdue() {
        // 8am today while "now" is noon: same calendar day => This Week per definition
        assertEquals(TaskCategory.THIS_WEEK, cat(at(2026, Calendar.JULY, 15, 8, 0)))
    }

    @Test fun laterToday_isThisWeek() {
        assertEquals(TaskCategory.THIS_WEEK, cat(at(2026, Calendar.JULY, 15, 23, 0)))
    }

    @Test fun endOfThisWeek_sunday_isThisWeek() {
        assertEquals(TaskCategory.THIS_WEEK, cat(at(2026, Calendar.JULY, 19, 23, 0)))
    }

    @Test fun startOfNextWeek_monday_isNextWeek() {
        assertEquals(TaskCategory.NEXT_WEEK, cat(at(2026, Calendar.JULY, 20, 0, 0)))
    }

    @Test fun endOfNextWeek_sunday_isNextWeek() {
        assertEquals(TaskCategory.NEXT_WEEK, cat(at(2026, Calendar.JULY, 26, 23, 0)))
    }

    @Test fun weekAfterNext_monday_isSomeday() {
        assertEquals(TaskCategory.SOMEDAY, cat(at(2026, Calendar.JULY, 27, 9, 0)))
    }

    @Test fun farFuture_isSomeday() {
        assertEquals(TaskCategory.SOMEDAY, cat(at(2026, Calendar.DECEMBER, 1, 9, 0)))
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
            task(1, at(2026, Calendar.JULY, 22, 9, 0)),   // Next Week
            task(2, null),                                 // Someday
            task(3, at(2026, Calendar.JULY, 14, 9, 0)),   // Overdue
            task(4, at(2026, Calendar.JULY, 16, 9, 0)),   // This Week
        )
        val groups = TaskGrouping.groupActive(tasks, now)
        assertEquals(
            listOf(
                TaskCategory.OVERDUE,
                TaskCategory.THIS_WEEK,
                TaskCategory.NEXT_WEEK,
                TaskCategory.SOMEDAY,
            ),
            groups.map { it.first },
        )
        assertEquals(listOf(3L), groups[0].second.map { it.id })
        assertEquals(listOf(4L), groups[1].second.map { it.id })
        assertEquals(listOf(1L), groups[2].second.map { it.id })
        assertEquals(listOf(2L), groups[3].second.map { it.id })
    }

    @Test fun groupActive_omitsEmptyBuckets() {
        val tasks = listOf(
            task(1, null),                                 // Someday
            task(2, at(2026, Calendar.JULY, 16, 9, 0)),   // This Week
        )
        val groups = TaskGrouping.groupActive(tasks, now)
        assertEquals(listOf(TaskCategory.THIS_WEEK, TaskCategory.SOMEDAY), groups.map { it.first })
    }

    @Test fun groupActive_preservesInputOrderWithinBucket() {
        // Two This-Week tasks provided in a specific (already-sorted) order
        val tasks = listOf(
            task(10, at(2026, Calendar.JULY, 18, 9, 0)),
            task(11, at(2026, Calendar.JULY, 16, 9, 0)),
        )
        val groups = TaskGrouping.groupActive(tasks, now)
        assertEquals(1, groups.size)
        assertEquals(TaskCategory.THIS_WEEK, groups[0].first)
        assertEquals(listOf(10L, 11L), groups[0].second.map { it.id }) // order preserved
    }

    @Test fun groupActive_emptyInput_returnsEmpty() {
        assertEquals(emptyList<Pair<TaskCategory, List<Task>>>(), TaskGrouping.groupActive(emptyList(), now))
    }
}
