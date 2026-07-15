package com.example.jotjot.ui

import com.example.jotjot.data.Priority
import com.example.jotjot.data.Recurrence
import com.example.jotjot.data.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskListLogicTest {

    private fun task(
        id: Long,
        title: String,
        notes: String? = null,
        createdAt: Long = 0L,
        dueDate: Long? = null,
        priority: Priority = Priority.MEDIUM,
        recurrence: Recurrence = Recurrence.NONE,
    ) = Task(
        id = id,
        title = title,
        notes = notes,
        isCompleted = false,
        createdAt = createdAt,
        dueDate = dueDate,
        priority = priority,
        recurrence = recurrence,
    )

    // ---- filtering ----

    @Test
    fun blankQuery_returnsAllTasks() {
        val tasks = listOf(task(1, "Buy milk"), task(2, "Call Alex"))
        val result = TaskListLogic.filterAndSort(
            tasks, SortOrder.CREATION_DATE, SortDirection.ASCENDING, "   ",
        )
        assertEquals(2, result.size)
    }

    @Test
    fun query_matchesTitleCaseInsensitively() {
        val tasks = listOf(task(1, "Buy MILK"), task(2, "Call Alex"))
        val result = TaskListLogic.filterAndSort(
            tasks, SortOrder.CREATION_DATE, SortDirection.ASCENDING, "milk",
        )
        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun query_matchesNotes() {
        val tasks = listOf(
            task(1, "Groceries", notes = "remember the oat milk"),
            task(2, "Gym"),
        )
        val result = TaskListLogic.filterAndSort(
            tasks, SortOrder.CREATION_DATE, SortDirection.ASCENDING, "OAT",
        )
        assertEquals(listOf(1L), result.map { it.id })
    }

    // ---- sorting ----

    @Test
    fun sortByCreationDate_ascending() {
        val tasks = listOf(
            task(1, "b", createdAt = 200),
            task(2, "a", createdAt = 100),
            task(3, "c", createdAt = 300),
        )
        val result = TaskListLogic.filterAndSort(
            tasks, SortOrder.CREATION_DATE, SortDirection.ASCENDING, "",
        )
        assertEquals(listOf(2L, 1L, 3L), result.map { it.id })
    }

    @Test
    fun sortByCreationDate_descendingReversesAscending() {
        val tasks = listOf(
            task(1, "b", createdAt = 200),
            task(2, "a", createdAt = 100),
            task(3, "c", createdAt = 300),
        )
        val result = TaskListLogic.filterAndSort(
            tasks, SortOrder.CREATION_DATE, SortDirection.DESCENDING, "",
        )
        assertEquals(listOf(3L, 1L, 2L), result.map { it.id })
    }

    @Test
    fun sortByDueDate_nullsSortLastWhenAscending() {
        val tasks = listOf(
            task(1, "no due", dueDate = null),
            task(2, "later", dueDate = 500),
            task(3, "sooner", dueDate = 100),
        )
        val result = TaskListLogic.filterAndSort(
            tasks, SortOrder.DUE_DATE, SortDirection.ASCENDING, "",
        )
        assertEquals(listOf(3L, 2L, 1L), result.map { it.id })
    }

    @Test
    fun sortByPriority_ascendingIsLowToHigh() {
        val tasks = listOf(
            task(1, "high", priority = Priority.HIGH),
            task(2, "low", priority = Priority.LOW),
            task(3, "medium", priority = Priority.MEDIUM),
        )
        val result = TaskListLogic.filterAndSort(
            tasks, SortOrder.PRIORITY, SortDirection.ASCENDING, "",
        )
        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    @Test
    fun filterAndSort_doesNotMutateInput() {
        val tasks = listOf(task(1, "b", createdAt = 200), task(2, "a", createdAt = 100))
        val snapshot = tasks.map { it.id }
        TaskListLogic.filterAndSort(tasks, SortOrder.CREATION_DATE, SortDirection.DESCENDING, "")
        assertEquals(snapshot, tasks.map { it.id })
    }
}
