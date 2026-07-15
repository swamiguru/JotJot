package com.example.jotjot.ui

import com.example.jotjot.data.Task

/**
 * Pure, Android-free logic for filtering/sorting the task list. Kept separate
 * from [TaskViewModel] so it can be unit tested on the JVM without an emulator
 * or Robolectric. Recurrence date math lives in
 * [com.example.jotjot.data.RecurrenceCalculator].
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
}
