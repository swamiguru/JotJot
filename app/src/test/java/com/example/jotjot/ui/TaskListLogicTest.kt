package com.example.jotjot.ui

import com.example.jotjot.data.Priority
import com.example.jotjot.data.Recurrence
import com.example.jotjot.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.util.Calendar
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
        // sooner, later, then the null-due task last
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

    // ---- recurrence ----

    @Test
    fun recurrenceNone_returnsSameInstant() {
        val now = System.currentTimeMillis()
        assertEquals(now, TaskListLogic.calculateNextDueDate(now, Recurrence.NONE))
    }

    @Test
    fun recurrenceDaily_addsOneDay() {
        val base = calendarAt(2026, Calendar.JANUARY, 10, 9, 0)
        val next = TaskListLogic.calculateNextDueDate(base, Recurrence.DAILY)
        assertEquals(base + DAY_MS, next)
    }

    @Test
    fun recurrenceWeekly_addsSevenDays() {
        val base = calendarAt(2026, Calendar.JANUARY, 10, 9, 0)
        val next = TaskListLogic.calculateNextDueDate(base, Recurrence.WEEKLY)
        assertEquals(base + 7 * DAY_MS, next)
    }

    @Test
    fun recurrenceMonthly_advancesCalendarMonth() {
        val base = calendarAt(2026, Calendar.JANUARY, 10, 9, 0)
        val next = TaskListLogic.calculateNextDueDate(base, Recurrence.MONTHLY)
        val cal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH))
        assertEquals(10, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun recurrenceYearly_advancesToNextYear() {
        val base = calendarAt(2026, Calendar.MARCH, 3, 8, 30)
        val next = TaskListLogic.calculateNextDueDate(base, Recurrence.YEARLY)
        val cal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(2027, cal.get(Calendar.YEAR))
        assertTrue(next > base)
    }

    private fun calendarAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
