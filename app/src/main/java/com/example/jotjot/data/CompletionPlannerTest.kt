package com.example.jotjot.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Validates CompletionPlanner: the pure decision logic behind
 * TaskDao.completeTaskAndSpawnNext. This is the layer responsible for two
 * bugs an Android instrumented test can't easily exercise: (1) completing an
 * already-completed task must be a no-op, and (2) a recurring completion must
 * spawn exactly one next occurrence, never more, regardless of how many times
 * completion runs against a stale/incompletely-applied snapshot.
 *
 * Timezone pinned to UTC to match RecurrenceCalculatorTest's convention, since
 * plan() delegates date math to RecurrenceCalculator.
 */
class CompletionPlannerTest {

    private val tz = TimeZone.getTimeZone("UTC")
    private val originalTz = TimeZone.getDefault()

    @Before fun pinTimezone() = TimeZone.setDefault(tz)

    @After fun restoreTimezone() = TimeZone.setDefault(originalTz)

    private fun at(y: Int, m: Int, d: Int, h: Int = 9, min: Int = 0): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(y, m, d, h, min, 0)
        }.timeInMillis

    // ---------- non-recurring ----------

    @Test
    fun nonRecurring_noDueDate_completesWithoutSpawning() {
        val task = Task(id = 1, title = "Buy milk", isCompleted = false)
        val plan = CompletionPlanner.plan(task)
        assertTrue(plan is CompletionPlanner.Plan.Complete)
        assertNull((plan as CompletionPlanner.Plan.Complete).spawnedNext)
    }

    @Test
    fun nonRecurring_withDueDate_completesWithoutSpawning() {
        val due = at(2026, Calendar.MARCH, 10)
        val task = Task(id = 1, title = "Pay rent", isCompleted = false, dueDate = due)
        val plan = CompletionPlanner.plan(task)
        assertTrue(plan is CompletionPlanner.Plan.Complete)
        assertNull((plan as CompletionPlanner.Plan.Complete).spawnedNext)
    }

    // ---------- recurring: spawns exactly one next occurrence ----------

    @Test
    fun recurringDaily_spawnsNextOccurrence_oneCalendarDayLater() {
        val due = at(2026, Calendar.MARCH, 10, 9, 0)
        val now = at(2026, Calendar.MARCH, 10, 8, 0) // before the due time -> single step
        val task = Task(id = 1, title = "Take vitamins", isCompleted = false, dueDate = due, recurrence = Recurrence.DAILY)

        val plan = CompletionPlanner.plan(task, now) as CompletionPlanner.Plan.Complete
        val spawned = plan.spawnedNext
        requireNotNull(spawned)

        // Exactly one occurrence spawned, not completed itself, id reset for insertion.
        assertEquals(0L, spawned.id)
        assertEquals(false, spawned.isCompleted)
        assertEquals(RecurrenceCalculator.nextFutureDueDate(due, Recurrence.DAILY, now), spawned.dueDate)
        assertEquals(task.title, spawned.title)
        assertEquals(task.recurrence, spawned.recurrence)
        assertEquals(task.id, spawned.spawnedFromId)
    }

    @Test
    fun recurringWeekly_spawnedDueDate_isStrictlyAfterNow() {
        val due = at(2026, Calendar.MARCH, 10, 9, 0)
        val now = at(2026, Calendar.MARCH, 10, 9, 0) // exactly on the due instant
        val task = Task(id = 1, title = "Team sync", isCompleted = false, dueDate = due, recurrence = Recurrence.WEEKLY)

        val plan = CompletionPlanner.plan(task, now) as CompletionPlanner.Plan.Complete
        val spawned = requireNotNull(plan.spawnedNext)

        assertTrue("spawned occurrence must not be immediately due again", spawned.dueDate!! > now)
    }

    @Test
    fun recurringOverdue_catchesUpInstead_ofSpawningAnImmediatelyDueTask() {
        // Task was due weeks ago (e.g. app was uninstalled/offline) -- completing it
        // now must not spawn something already overdue, which would refire instantly.
        val due = at(2026, Calendar.JANUARY, 1, 9, 0)
        val now = at(2026, Calendar.MARCH, 1, 9, 0)
        val task = Task(id = 1, title = "Water plants", isCompleted = false, dueDate = due, recurrence = Recurrence.WEEKLY)

        val plan = CompletionPlanner.plan(task, now) as CompletionPlanner.Plan.Complete
        val spawned = requireNotNull(plan.spawnedNext)

        assertTrue(spawned.dueDate!! > now)
    }

    // ---------- idempotency: the actual bug this file exists to catch ----------

    @Test
    fun alreadyCompletedTask_isANoOp_evenIfRecurring() {
        val due = at(2026, Calendar.MARCH, 10, 9, 0)
        val task = Task(id = 1, title = "Standup", isCompleted = true, dueDate = due, recurrence = Recurrence.DAILY)

        val plan = CompletionPlanner.plan(task)

        assertEquals(CompletionPlanner.Plan.AlreadyCompleted, plan)
    }

    @Test
    fun completingTwiceInARow_onlyTheFirstCallSpawnsANextOccurrence() {
        // Simulates the double-tap / redelivered-broadcast scenario: the same
        // logical completion is attempted twice. The second attempt must see the
        // already-applied result of the first (isCompleted = true) and no-op,
        // rather than spawning a second duplicate next occurrence.
        val due = at(2026, Calendar.MARCH, 10, 9, 0)
        val original = Task(id = 1, title = "Take out trash", isCompleted = false, dueDate = due, recurrence = Recurrence.WEEKLY)

        val firstPlan = CompletionPlanner.plan(original) as CompletionPlanner.Plan.Complete
        assertTrue(firstPlan.spawnedNext != null)

        // What the DB would look like immediately after the first call committed.
        val afterFirstCompletion = original.copy(isCompleted = true)

        val secondPlan = CompletionPlanner.plan(afterFirstCompletion)
        assertEquals(CompletionPlanner.Plan.AlreadyCompleted, secondPlan)
    }

    // ---------- UncompletionPlanner: the "mark as active" / undo path ----------

    @Test
    fun uncompletingAnActiveTask_isANoOp() {
        val task = Task(id = 1, title = "Buy milk", isCompleted = false)
        assertEquals(UncompletionPlanner.Plan.AlreadyActive, UncompletionPlanner.plan(task))
    }

    @Test
    fun uncompletingACompletedTask_proceeds() {
        val task = Task(id = 1, title = "Buy milk", isCompleted = true)
        assertEquals(UncompletionPlanner.Plan.Uncomplete, UncompletionPlanner.plan(task))
    }

    @Test
    fun uncompletingTwiceInARow_secondCallIsANoOp() {
        // Mirrors completingTwiceInARow_*: simulates a double-undo, or an undo
        // racing a second uncomplete attempt for the same task.
        val task = Task(id = 1, title = "Buy milk", isCompleted = true)

        val firstPlan = UncompletionPlanner.plan(task)
        assertEquals(UncompletionPlanner.Plan.Uncomplete, firstPlan)

        // What the DB would look like immediately after the first call committed.
        val afterFirstUncomplete = task.copy(isCompleted = false)
        val secondPlan = UncompletionPlanner.plan(afterFirstUncomplete)
        assertEquals(UncompletionPlanner.Plan.AlreadyActive, secondPlan)
    }
}
