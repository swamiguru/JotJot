package com.example.jotjot.data

import java.util.Calendar

/**
 * Pure date math for recurring tasks. Lives in the data layer so both the
 * repository (completion flow) and the UI can share it, and so it is unit
 * testable on the JVM without Android.
 */
object RecurrenceCalculator {

    /**
     * Advances [currentDueDate] by exactly one step of [recurrence].
     * [Recurrence.NONE] returns the input unchanged. Uses [Calendar.add] so the
     * wall-clock time of day and the app's "has time" millisecond sentinel are
     * preserved, and month/year overflow clamps to the last valid day.
     */
    fun calculateNextDueDate(currentDueDate: Long, recurrence: Recurrence): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentDueDate }
        when (recurrence) {
            Recurrence.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            Recurrence.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            Recurrence.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            Recurrence.YEARLY -> calendar.add(Calendar.YEAR, 1)
            Recurrence.NONE -> {}
        }
        return calendar.timeInMillis
    }

    /**
     * Returns the next occurrence of a recurring task that falls strictly after
     * [now]. Stepping repeatedly (rather than a single step) means an overdue
     * recurring task "catches up" to the next future slot instead of producing a
     * still-overdue date that would fire a reminder immediately. Each step keeps
     * the original time of day and the millisecond sentinel intact.
     *
     * For [Recurrence.NONE] the input [dueDate] is returned unchanged.
     */
    fun nextFutureDueDate(
        dueDate: Long,
        recurrence: Recurrence,
        now: Long = System.currentTimeMillis(),
    ): Long {
        if (recurrence == Recurrence.NONE) return dueDate
        var next = calculateNextDueDate(dueDate, recurrence)
        var guard = 0
        // Guard bounds the loop (e.g. a task years overdue) while never blocking.
        while (next <= now && guard < 10_000) {
            next = calculateNextDueDate(next, recurrence)
            guard++
        }
        return next
    }
}
