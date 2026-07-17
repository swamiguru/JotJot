package com.example.jotjot.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueDate IS NOT NULL")
    suspend fun getActiveTasksWithReminders(): List<Task>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDate ASC")
    suspend fun getActiveTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE spawnedFromId = :originalId LIMIT 1")
    suspend fun getSpawnedFrom(originalId: Long): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    /**
     * Completes task [taskId] and, for a recurring task, spawns its next
     * occurrence -- atomically, and idempotently.
     *
     * This is the single source of truth for completion (called by both the
     * UI and the notification action) specifically so that two completion
     * attempts racing each other -- a double-tapped "Mark Completed" action,
     * a redelivered broadcast, or the UI and a notification action firing at
     * the same moment -- can't both see the task as "not yet completed" and
     * both spawn a next occurrence. The current row is re-read from the
     * database *inside* this transaction, so if one caller's transaction has
     * already committed the completion, the other sees isCompleted already
     * true and becomes a no-op (CompletionOutcome.AlreadyCompleted) instead
     * of creating a duplicate. Room serializes writer transactions against a
     * single database connection, so this re-check is race-free even though
     * the two calls originate from independent coroutines/processes.
     */
    @Transaction
    suspend fun completeTaskAndSpawnNext(taskId: Long): CompletionOutcome {
        val current = getTaskById(taskId) ?: return CompletionOutcome.NotFound
        return when (val plan = CompletionPlanner.plan(current)) {
            is CompletionPlanner.Plan.AlreadyCompleted -> CompletionOutcome.AlreadyCompleted
            is CompletionPlanner.Plan.Complete -> {
                val spawned = plan.spawnedNext?.let { next ->
                    val newId = insertTask(next)
                    next.copy(id = newId)
                }
                updateTask(current.copy(isCompleted = true))
                CompletionOutcome.Completed(current, spawned)
            }
        }
    }

    /**
     * Reverses [completeTaskAndSpawnNext]: marks [taskId] active again and, if
     * completing it had spawned a next occurrence, removes that occurrence --
     * atomically, and idempotently.
     *
     * Two things this closes that a naive "just flip isCompleted back" doesn't:
     *
     * 1. The current row is re-read *inside* this transaction rather than
     *    trusting a caller-supplied snapshot, so un-completing a task can't
     *    stomp on other fields (title/notes/etc.) that changed elsewhere since
     *    that snapshot was taken, and a double-undo (or an undo racing another
     *    uncomplete) sees the already-applied result and no-ops instead of
     *    running twice.
     * 2. The spawned occurrence is found via the durable [Task.spawnedFromId]
     *    link rather than an in-memory map, so cleanup works even after the
     *    app process has restarted -- e.g. completing a recurring task, fully
     *    closing the app, then reopening it and marking that task active again
     *    from the Completed section still removes the occurrence it spawned,
     *    instead of leaving both rows active as two copies of the same task.
     */
    @Transaction
    suspend fun uncompleteTaskAndRemoveSpawned(taskId: Long): UncompleteOutcome {
        val current = getTaskById(taskId) ?: return UncompleteOutcome.NotFound
        return when (UncompletionPlanner.plan(current)) {
            UncompletionPlanner.Plan.AlreadyActive -> UncompleteOutcome.AlreadyActive
            UncompletionPlanner.Plan.Uncomplete -> {
                updateTask(current.copy(isCompleted = false))
                val spawned = getSpawnedFrom(taskId)
                spawned?.let { deleteTask(it) }
                UncompleteOutcome.Uncompleted(current, spawned)
            }
        }
    }
}
