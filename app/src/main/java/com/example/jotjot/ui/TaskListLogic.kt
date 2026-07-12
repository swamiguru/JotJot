package com.example.jotjot.ui

import com.example.jotjot.data.Recurrence
import com.example.jotjot.data.Task
import java.util.Calendar

/**
 * Pure, Android-free logic for filtering/sorting the task list and computing
 * recurrence dates. Kept separate from [TaskViewModel] so it can be unit tested
 * on the JVM without an emulator or Robolectric.
 */
object TaskListLogic {

    /**
     * Filters tasks by a search [query] (matched against title and notes,
     * case-insensitively) and sorts them per [sortOrder] and [sortDirection].
     */
    fun filterAndSort(
        tasks: List<Task>,
        sortOrder: SortOrder,
        sortDirection: SortDirection,
        query: String,
    ): List<Task> {
        val filtered = if (query.isBlank()) {
            tasks
        } else {
            tasks.filter {
                it.title.contains(query, ignoreCase = true) ||
                    (it.notes?.contains(query, ignoreCase = true) ?: false)
            }
        }

        val sorted = when (sortOrder) {
            SortOrder.CREATION_DATE -> filtered.sortedBy { it.createdAt }
            SortOrder.DUE_DATE ->
                filtered.sortedWith(compareBy<Task> { it.dueDate == null }.thenBy { it.dueDate })
            SortOrder.PRIORITY -> filtered.sortedBy { it.priority }
        }

        return if (sortDirection == SortDirection.DESCENDING) sorted.reversed() else sorted
    }

    /**
     * Given a due date and a recurrence rule, returns the next occurrence's due date.
     * A [Recurrence.NONE] rule returns the input unchanged.
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
}
