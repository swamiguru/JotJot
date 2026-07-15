package com.example.jotjot.ui

import com.example.jotjot.data.Task
import java.util.Calendar

/**
 * Time-based buckets used to group active (incomplete) tasks for display.
 * [displayName] is the section header shown in the UI.
 */
enum class TaskCategory(val displayName: String) {
    OVERDUE("Overdue"),
    THIS_WEEK("This Week"),
    NEXT_WEEK("Next Week"),
    SOMEDAY("Someday"),
}

/**
 * Pure, Android-free logic that assigns each task to a [TaskCategory] and groups
 * a list of active tasks into ordered, non-empty buckets. Weeks start on Monday.
 * All calculations use the JVM default timezone (mirroring the device), and are
 * unit tested on the JVM.
 *
 * Bucket definitions (relative to today):
 *  - OVERDUE   : due on a calendar day before today
 *  - THIS_WEEK : due today through the end of the current (Mon–Sun) week
 *  - NEXT_WEEK : due during the following Mon–Sun week
 *  - SOMEDAY   : no due date, or due beyond next week
 */
object TaskGrouping {

    private val DISPLAY_ORDER = listOf(
        TaskCategory.OVERDUE,
        TaskCategory.THIS_WEEK,
        TaskCategory.NEXT_WEEK,
        TaskCategory.SOMEDAY,
    )

    /** Returns the bucket a task with [dueDate] belongs to. Null due date => SOMEDAY. */
    fun categorize(dueDate: Long?, now: Long = System.currentTimeMillis()): TaskCategory {
        if (dueDate == null) return TaskCategory.SOMEDAY

        val todayStart = startOfDay(now)
        if (dueDate < todayStart) return TaskCategory.OVERDUE

        val thisWeekStart = startOfWeekMonday(now)
        val nextWeekStart = addDays(thisWeekStart, 7)
        val weekAfterNextStart = addDays(thisWeekStart, 14)

        return when {
            dueDate < nextWeekStart -> TaskCategory.THIS_WEEK
            dueDate < weekAfterNextStart -> TaskCategory.NEXT_WEEK
            else -> TaskCategory.SOMEDAY
        }
    }

    /**
     * Groups already-active, already-sorted [activeTasks] into display buckets,
     * preserving the incoming order within each bucket and returning only the
     * non-empty buckets in fixed display order.
     */
    fun groupActive(
        activeTasks: List<Task>,
        now: Long = System.currentTimeMillis(),
    ): List<Pair<TaskCategory, List<Task>>> {
        val byCategory = activeTasks.groupBy { categorize(it.dueDate, now) }
        return DISPLAY_ORDER.mapNotNull { category ->
            byCategory[category]?.let { category to it }
        }
    }

    private fun startOfDay(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /** Start (00:00) of the Monday on or before [millis]. */
    private fun startOfWeekMonday(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // DAY_OF_WEEK: SUNDAY=1 .. SATURDAY=7; MONDAY=2. Distance back to Monday:
        val daysFromMonday = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        return cal.timeInMillis
    }

    /** Adds [days] calendar days, preserving wall-clock time across DST. */
    private fun addDays(millis: Long, days: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis
}
