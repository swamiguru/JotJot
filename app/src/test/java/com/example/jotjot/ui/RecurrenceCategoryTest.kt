package com.example.jotjot.ui

import com.example.jotjot.data.Recurrence
import com.example.jotjot.data.RecurrenceCalculator
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import java.util.Calendar
import java.util.TimeZone
import org.junit.Test

/**
 * Cross-module audit of what happens when a recurring task is completed:
 * the next instance's due date (RecurrenceCalculator.nextFutureDueDate, the same
 * call TaskRepository.completeTask makes) is fed into TaskGrouping.categorize to
 * see which section the new instance lands in.
 *
 * Reference "now" is Wed 2026-07-15 (this week Mon 07-13 .. Sun 07-19;
 * next week Mon 07-20 .. Sun 07-26). Timezone pinned to UTC.
 */
class RecurrenceCategoryTest {

    private val tz = TimeZone.getTimeZone("UTC")
    private val originalTz = TimeZone.getDefault()

    @Before fun pin() = TimeZone.setDefault(tz)

    @After fun restore() = TimeZone.setDefault(originalTz)

    // ms = 1 => a task with a specific time-of-day (the app's has-time sentinel).
    private fun at(y: Int, mo: Int, d: Int, h: Int = 9, mi: Int = 0, ms: Int = 1): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(y, mo, d, h, mi, 0)
            set(Calendar.MILLISECOND, ms)
        }.timeInMillis

    /** Bucket the next instance lands in after completing a task due [due] with [rec], at [now]. */
    private fun nextBucket(due: Long, rec: Recurrence, now: Long): TaskCategory {
        val next = RecurrenceCalculator.nextFutureDueDate(due, rec, now)
        return TaskGrouping.categorize(next, now)
    }

    private val nowWed = at(2026, Calendar.JULY, 15, 12, 0) // Wednesday noon
    private val dueTodayWed = at(2026, Calendar.JULY, 15, 9, 0) // due today 9am

    @Test fun daily_completedMidweek_nextInstanceIsThisWeek() {
        // Daily -> tomorrow (Thu) -> This Week
        assertEquals(TaskCategory.THIS_WEEK, nextBucket(dueTodayWed, Recurrence.DAILY, nowWed))
    }

    @Test fun weekly_completedMidweek_nextInstanceIsNextWeek() {
        // Weekly -> +7 days (next Wed) -> Next Week
        assertEquals(TaskCategory.NEXT_WEEK, nextBucket(dueTodayWed, Recurrence.WEEKLY, nowWed))
    }

    @Test fun monthly_nextInstanceIsSomeday() {
        // Monthly -> Aug 15 -> beyond next week -> Someday
        assertEquals(TaskCategory.SOMEDAY, nextBucket(dueTodayWed, Recurrence.MONTHLY, nowWed))
    }

    @Test fun yearly_nextInstanceIsSomeday() {
        // Yearly -> 2027 -> Someday
        assertEquals(TaskCategory.SOMEDAY, nextBucket(dueTodayWed, Recurrence.YEARLY, nowWed))
    }

    @Test fun daily_completedOnSunday_nextInstanceIsNextWeek() {
        // Edge: completing a daily task on Sunday -> tomorrow is Monday -> Next Week
        val nowSun = at(2026, Calendar.JULY, 19, 12, 0) // Sunday noon
        val dueSun = at(2026, Calendar.JULY, 19, 9, 0)
        assertEquals(TaskCategory.NEXT_WEEK, nextBucket(dueSun, Recurrence.DAILY, nowSun))
    }

    @Test fun overdueDaily_catchesUp_nextInstanceIsThisWeek() {
        // A daily task 3 days overdue, completed today -> next future slot is tomorrow -> This Week
        val overdueDue = at(2026, Calendar.JULY, 12, 9, 0)
        assertEquals(TaskCategory.THIS_WEEK, nextBucket(overdueDue, Recurrence.DAILY, nowWed))
    }

    @Test fun dateOnlyDaily_nextInstanceIsThisWeek() {
        // Date-only daily task due today (no specific time) -> tomorrow -> This Week
        val dateOnlyToday = at(2026, Calendar.JULY, 15, 0, 0, ms = 0)
        assertEquals(TaskCategory.THIS_WEEK, nextBucket(dateOnlyToday, Recurrence.DAILY, nowWed))
    }
}
