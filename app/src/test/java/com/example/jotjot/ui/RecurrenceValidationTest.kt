package com.example.jotjot.ui

import com.example.jotjot.data.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import java.util.Calendar
import java.util.TimeZone
import org.junit.Test

/**
 * Validation of TaskListLogic.calculateNextDueDate across the four recurrence
 * rules, including calendar edge cases (month-end clamping, leap years) and the
 * app's "has time" millisecond sentinel (ms == 1 means the due date carries a
 * specific time; ms == 0 means date-only).
 *
 * Tests use a fixed timezone so day/hour assertions are deterministic.
 */
class RecurrenceValidationTest {

    private val tz = TimeZone.getTimeZone("America/New_York")

    private fun at(y: Int, m: Int, d: Int, h: Int = 9, min: Int = 0, ms: Int = 0): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(y, m, d, h, min, 0)
            set(Calendar.MILLISECOND, ms)
        }.timeInMillis

    private fun field(millis: Long, f: Int): Int =
        Calendar.getInstance(tz).apply { timeInMillis = millis }.get(f)

    // ---------- basic correctness (expected behavior) ----------

    @Test
    fun daily_advancesOneCalendarDay_preservingTime() {
        val base = at(2026, Calendar.MARCH, 10, 9, 30)
        val next = TaskListLogic.calculateNextDueDate(base, Recurrence.DAILY)
        assertEquals(11, field(next, Calendar.DAY_OF_MONTH))
        assertEquals(9, field(next, Calendar.HOUR_OF_DAY))
        assertEquals(30, field(next, Calendar.MINUTE))
    }

    @Test
    fun weekly_advancesSevenDays_sameWeekday() {
        val base = at(2026, Calendar.MARCH, 10, 9, 0)
        val next = TaskListLogic.calculateNextDueDate(base, Recurrence.WEEKLY)
        assertEquals(field(base, Calendar.DAY_OF_WEEK), field(next, Calendar.DAY_OF_WEEK))
        assertEquals(17, field(next, Calendar.DAY_OF_MONTH))
    }

    @Test
    fun monthly_advancesOneMonth_sameDay() {
        val base = at(2026, Calendar.JANUARY, 15, 8, 0)
        val next = TaskListLogic.calculateNextDueDate(base, Recurrence.MONTHLY)
        assertEquals(Calendar.FEBRUARY, field(next, Calendar.MONTH))
        assertEquals(15, field(next, Calendar.DAY_OF_MONTH))
    }

    @Test
    fun yearly_advancesOneYear_sameMonthDay() {
        val base = at(2026, Calendar.JULY, 4, 12, 0)
        val next = TaskListLogic.calculateNextDueDate(base, Recurrence.YEARLY)
        assertEquals(2027, field(next, Calendar.YEAR))
        assertEquals(Calendar.JULY, field(next, Calendar.MONTH))
        assertEquals(4, field(next, Calendar.DAY_OF_MONTH))
    }

    // ---------- has-time sentinel preservation ----------

    @Test
    fun timedTask_keepsMillisecondSentinel_1() {
        val base = at(2026, Calendar.MARCH, 10, 9, 30, ms = 1)
        val next = TaskListLogic.calculateNextDueDate(base, Recurrence.DAILY)
        assertEquals(1, field(next, Calendar.MILLISECOND)) // still flagged "has time"
    }

    @Test
    fun dateOnlyTask_keepsMillisecondSentinel_0() {
        val base = at(2026, Calendar.MARCH, 10, 0, 0, ms = 0)
        val next = TaskListLogic.calculateNextDueDate(base, Recurrence.WEEKLY)
        assertEquals(0, field(next, Calendar.MILLISECOND)) // still "date only"
    }

    // ---------- edge cases (documenting actual behavior) ----------

    /** Jan 31 has no counterpart in Feb, so Calendar clamps to Feb 28 (2026 is not a leap year). */
    @Test
    fun monthly_jan31_clampsToFeb28() {
        val base = at(2026, Calendar.JANUARY, 31, 9, 0)
        val next = TaskListLogic.calculateNextDueDate(base, Recurrence.MONTHLY)
        assertEquals(Calendar.FEBRUARY, field(next, Calendar.MONTH))
        assertEquals(28, field(next, Calendar.DAY_OF_MONTH))
    }

    /**
     * DRIFT: after Jan 31 clamps to Feb 28, the *next* monthly step is computed from
     * Feb 28 and lands on Mar 28 — NOT Mar 31. A "31st of every month" task silently
     * migrates to the 28th permanently.
     */
    @Test
    fun monthly_endOfMonthDrifts_afterClamp() {
        val jan31 = at(2026, Calendar.JANUARY, 31, 9, 0)
        val feb = TaskListLogic.calculateNextDueDate(jan31, Recurrence.MONTHLY)
        val mar = TaskListLogic.calculateNextDueDate(feb, Recurrence.MONTHLY)
        assertEquals(Calendar.MARCH, field(mar, Calendar.MONTH))
        assertNotEquals(31, field(mar, Calendar.DAY_OF_MONTH)) // drifted to 28, not back to 31
        assertEquals(28, field(mar, Calendar.DAY_OF_MONTH))
    }

    /** Feb 29 (2028 leap) has no counterpart in 2029, so yearly clamps to Feb 28. */
    @Test
    fun yearly_feb29_clampsToFeb28NextYear() {
        val leapDay = at(2028, Calendar.FEBRUARY, 29, 9, 0)
        val next = TaskListLogic.calculateNextDueDate(leapDay, Recurrence.YEARLY)
        assertEquals(2029, field(next, Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, field(next, Calendar.MONTH))
        assertEquals(28, field(next, Calendar.DAY_OF_MONTH))
    }
}
