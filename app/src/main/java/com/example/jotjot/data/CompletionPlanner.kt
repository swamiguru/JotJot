package com.example.jotjot.data

/**
 * Pure decision logic for what should happen when a task is completed. Kept
 * separate from Room/Android (mirrors [RecurrenceCalculator] and
 * [com.example.jotjot.ui.TaskListLogic]) so the exact bug class this guards
 * against -- completing the same task twice -- is unit testable on the JVM.
 *
 * The critical property: [plan] must be called with a value of [current] that
 * was read *inside* the same database transaction that will apply the result.
 * That is what makes the [Plan.AlreadyCompleted] guard actually safe against a
 * race -- e.g. a double-tapped "Mark Completed" notification action, or the
 * same broadcast being redelivered -- rather than just reducing its
 * likelihood. See [TaskDao.completeTaskAndSpawnNext], which is the only
 * intended caller.
 */
object CompletionPlanner {

    sealed class Plan {
        /**
         * [current] was already completed. The caller must do nothing further --
         * in particular it must NOT spawn another next-occurrence or touch
         * reminders again. This is what makes completion idempotent: calling it
         * twice on the same task produces one completion, not two.
         */
        object AlreadyCompleted : Plan()

        /**
         * [current] should be marked completed. [spawnedNext] is the next
         * occurrence to insert for a recurring task with a due date, or null for
         * a non-recurring task (or one without a due date).
         */
        data class Complete(val spawnedNext: Task?) : Plan()
    }

    fun plan(current: Task, now: Long = System.currentTimeMillis()): Plan {
        if (current.isCompleted) return Plan.AlreadyCompleted

        val spawnedNext = if (current.recurrence != Recurrence.NONE && current.dueDate != null) {
            current.copy(
                id = 0,
                isCompleted = false,
                dueDate = RecurrenceCalculator.nextFutureDueDate(current.dueDate, current.recurrence, now),
                createdAt = now,
                spawnedFromId = current.id,
            )
        } else {
            null
        }
        return Plan.Complete(spawnedNext)
    }
}

/**
 * Pure decision logic for the reverse of [CompletionPlanner]: un-completing a
 * task ("mark as active"), including removing the next occurrence it may have
 * spawned. Same idempotency shape as [CompletionPlanner] -- if the task is
 * already active, this is a no-op, which is what makes a double-undo (or an
 * undo racing a completion) safe rather than merely unlikely to misfire.
 */
object UncompletionPlanner {

    sealed class Plan {
        /** [current] is already active; caller must do nothing further. */
        object AlreadyActive : Plan()

        /** [current] should be marked active again. */
        object Uncomplete : Plan()
    }

    fun plan(current: Task): Plan = if (!current.isCompleted) Plan.AlreadyActive else Plan.Uncomplete
}

/**
 * Result of [TaskDao.completeTaskAndSpawnNext], surfaced to [TaskRepository] so
 * it knows which side effects (reminders, widget refresh) to apply.
 */
sealed class CompletionOutcome {
    object NotFound : CompletionOutcome()
    object AlreadyCompleted : CompletionOutcome()
    data class Completed(val original: Task, val spawned: Task?) : CompletionOutcome()
}

/**
 * Result of [TaskDao.uncompleteTaskAndRemoveSpawned], surfaced to
 * [TaskRepository] so it knows which side effects (reminders, widget refresh)
 * to apply.
 */
sealed class UncompleteOutcome {
    object NotFound : UncompleteOutcome()
    object AlreadyActive : UncompleteOutcome()
    data class Uncompleted(val original: Task, val removedSpawn: Task?) : UncompleteOutcome()
}
