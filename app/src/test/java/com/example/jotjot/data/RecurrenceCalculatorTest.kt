package com.example.jotjot.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import java.util.Calendar
import java.util.TimeZone
import org.junit.Test

/**
 * Validates RecurrenceCalculator: single-step advancement across the four rules,
 * calendar edge cases (month-end clamping, leap years), the "has time"
 * millisecond sentinel, and nextFutureDueDate's catch-up behavior for overdue
 * recurring tasks.
 *
 * RecurrenceCalculator uses the JVM default timezone (mirroring a real device),
 * so the test pins the default timezone to UTC for deterministic, DST-free
 * arithmetic and restores it afterward.
 */
class RecurrenceCalculatorTest {

    private val tz = TimeZone.getTimeZone("UTC")
    private val originalTz = TimeZone.getDefault()

    @Before fun pinTimezone() = TimeZone.setDefault(tz)

    @After fun restoreTimezone() = TimeZone.setDefault(originalTz)

    private fun at(y: Int, m: Int, d: Int, h: Int = 9, min: Int = 0, ms: Int = 0): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(y, m, d, h, min, 0)
            set(Calendar.MILLISECOND, ms)
        }.timeInMillis

    private fun field(millis: Long, f: Int): Int =
        Calendar.getInstance(tz).apply { timeInMillis = millis }.get(f)

    // ---------- single-step correctness ----------

    @Test
    fun daily_advancesOneCalendarDay_preservingTime() {
        val base = at(2026, Calendar.MARCH, 10, 9, 30)
        val next = RecurrenceCalculator.calculateNextDueDate(base, Recurrence.DAILY)
        assertEquals(11, field(next, Calendar.DAY_OF_MONTH))
        assertEquals(9, field(next, Calendar.HOUR_OF_DAY))
        assertEquals(30, field(next, Calendar.MINUTE))
    }

    @Test
    fun weekly_advancesSevenDays_sameWeekday() {
        val base = at(2026, Calendar.MARCH, 10, 9, 0)
        val next = RecurrenceCalculator.calculateNextDueDate(base, Recurrence.WEEKLY)
        assertEquals(field(base, Calendar.DAY_OF_WEEK), field(next, Calendar.DAY_OF_WEEK))
        assertEquals(17, field(next, Calendar.DAY_OF_MONTH))
    }

    @Test
    fun monthly_advancesOneMonth_sameDay() {
        val base = at(2026, Calendar.JANUARY, 15, 8, 0)
        val next = RecurrenceCalculator.calculateNextDueDate(base, Recurrence.MONTHLY)
        assertEquals(Calendar.FEBRUARY, field(next, Calendar.MONTH))
        assertEquals(15, field(next, Calendar.DAY_OF_MONTH))
    }

    @Test
    fun yearly_advancesOneYear_sameMonthDay() {
        val base = at(2026, Calendar.JULY, 4, 12, 0)
        val next = RecurrenceCalculator.calculateNextDueDate(base, Recurrence.YEARLY)
        assertEquals(2027, field(next, Calendar.YEAR))
        assertEquals(Calendar.JULY, field(next, Calendar.MONTH))
        assertEquals(4, field(next, Calendar.DAY_OF_MONTH))
    }

    @Test
    fun none_returnsSameInstant() {
        val now = at(2026, Calendar.MAY, 1, 10, 0)
        assertEquals(now, RecurrenceCalculator.calculateNextDueDate(now, Recurrence.NONE))
    }

    // ---------- has-time sentinel preservation ----------

    @Test
    fun timedTask_keepsMillisecondSentinel_1() {
        val base = at(2026, Calendar.MARCH, 10, 9, 30, ms = 1)
        val next = RecurrenceCalculator.calculateNextDueDate(base, Recurrence.DAILY)
        assertEquals(1, field(next, Calendar.MILLISECOND))
    }

    @Test
    fun dateOnlyTask_keepsMillisecondSentinel_0() {
        val base = at(2026, Calendar.MARCH, 10, 0, 0, ms = 0)
        val next = RecurrenceCalculator.calculateNextDueDate(base, Recurrence.WEEKLY)
        assertEquals(0, field(next, Calendar.MILLISECOND))
    }

    // ---------- month-end / leap-year edge cases ----------

    @Test
    fun monthly_jan31_clampsToFeb28() {
        val base = at(2026, Calendar.JANUARY, 31, 9, 0)
        val next = RecurrenceCalculator.calculateNextDueDate(base, Recurrence.MONTHLY)
        assertEquals(Calendar.FEBRUARY, field(next, Calendar.MONTH))
        assertEquals(28, field(next, Calendar.DAY_OF_MONTH))
    }

    @Test
    fun monthly_endOfMonthDrifts_afterClamp() {
        val jan31 = at(2026, Calendar.JANUARY, 31, 9, 0)
        val feb = RecurrenceCalculator.calculateNextDueDate(jan31, Recurrence.MONTHLY)
        val mar = RecurrenceCalculator.calculateNextDueDate(feb, Recurrence.MONTHLY)
        assertEquals(Calendar.MARCH, field(mar, Calendar.MONTH))
        assertNotEquals(31, field(mar, Calendar.DAY_OF_MONTH))
        assertEquals(28, field(mar, Calendar.DAY_OF_MONTH))
    }

    @Test
    fun yearly_feb29_clampsToFeb28NextYear() {
        val leapDay = at(2028, Calendar.FEBRUARY, 29, 9, 0)
        val next = RecurrenceCalculator.calculateNextDueDate(leapDay, Recurrence.YEARLY)
        assertEquals(2029, field(next, Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, field(next, Calendar.MONTH))
        assertEquals(28, field(next, Calendar.DAY_OF_MONTH))
    }

    // ---------- catch-up (nextFutureDueDate) ----------

    @Test
    fun nextFuture_nonOverdue_returnsSingleStep() {
        val now = at(2026, Calendar.MARCH, 10, 8, 0)     // before the 9:00 due time
        val due = at(2026, Calendar.MARCH, 10, 9, 0)
        val next = RecurrenceCalculator.nextFutureDueDate(due, Recurrence.DAILY, now)
        assertEquals(11, field(next, Calendar.DAY_OF_MONTH)) // tomorrow, one step
        assertEquals(9, field(next, Calendar.HOUR_OF_DAY))
    }

    @Test
    fun nextFuture_overdueDaily_catchesUpToNextFutureSlot() {
        val due = at(2026, Calendar.MARCH, 5, 9, 0)       // 5 days before now
        val now = at(2026, Calendar.MARCH, 10, 9, 0)      // exactly on a slot -> must go past it
        val next = RecurrenceCalculator.nextFutureDueDate(due, Recurrence.DAILY, now)
        assertEquals(11, field(next, Calendar.DAY_OF_MONTH)) // first slot strictly after now
        assertEquals(9, field(next, Calendar.HOUR_OF_DAY))   // original time preserved
        assertTrue(next > now)
    }

    @Test
    fun nextFuture_overdueMonthly_catchesUp() {
        val due = at(2026, Calendar.JANUARY, 15, 9, 0)
        val now = at(2026, Calendar.APRIL, 20, 9, 0)
        val next = RecurrenceCalculator.nextFutureDueDate(due, Recurrence.MONTHLY, now)
        assertEquals(Calendar.MAY, field(next, Calendar.MONTH)) // Feb,Mar,Apr are past -> May
        assertEquals(15, field(next, Calendar.DAY_OF_MONTH))
        assertTrue(next > now)
    }

    @Test
    fun nextFuture_overdueTimed_preservesSentinel() {
        val due = at(2026, Calendar.MARCH, 1, 9, 0, ms = 1)
        val now = at(2026, Calendar.MARCH, 10, 12, 0)
        val next = RecurrenceCalculator.nextFutureDueDate(due, Recurrence.DAILY, now)
        assertEquals(1, field(next, Calendar.MILLISECOND))
        assertTrue(next > now)
    }

    @Test
    fun nextFuture_none_returnsInputUnchanged() {
        val due = at(2026, Calendar.MARCH, 1, 9, 0)
        val now = at(2026, Calendar.MARCH, 10, 9, 0)
        assertEquals(due, RecurrenceCalculator.nextFutureDueDate(due, Recurrence.NONE, now))
    }
}
