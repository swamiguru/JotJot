package com.example.jotjot.data

import android.content.Context
import com.example.jotjot.notification.ReminderManager
import com.example.jotjot.widget.TaskWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val reminderManager: ReminderManager,
    private val context: Context
) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun insert(task: Task) {
        val id = taskDao.insertTask(task)
        if (task.dueDate != null) {
            reminderManager.scheduleReminder(task.copy(id = id))
        }
        TaskWidget().updateAll(context)
    }

    suspend fun update(task: Task) {
        taskDao.updateTask(task)
        if (task.isCompleted || task.dueDate == null) {
            reminderManager.cancelReminder(task.id)
        } else {
            reminderManager.scheduleReminder(task)
        }
        TaskWidget().updateAll(context)
    }

    suspend fun delete(task: Task) {
        taskDao.deleteTask(task)
        reminderManager.cancelReminder(task.id)
        TaskWidget().updateAll(context)
    }

    /**
     * Marks the task identified by [taskId] as completed. If it is a recurring
     * task with a due date, the next occurrence is created in the same atomic
     * step, with its due date advanced to the next slot strictly after now (so
     * an overdue task catches up instead of immediately re-firing). Returns the
     * newly created next occurrence (with its database id) so callers can offer
     * an undo, or null if nothing was spawned -- including when the task was
     * already completed by the time this ran (see [TaskDao.completeTaskAndSpawnNext]).
     *
     * This is the single source of truth for completion, shared by the UI and
     * the notification action, so recurrence -- and the idempotency guard
     * against completing the same task twice -- behaves identically everywhere.
     * Callers should pass the task's id rather than a possibly-stale [Task]
     * snapshot, since the up-to-date row is what the idempotency check relies on.
     */
    suspend fun completeTask(taskId: Long): Task? {
        return when (val outcome = taskDao.completeTaskAndSpawnNext(taskId)) {
            is CompletionOutcome.Completed -> {
                reminderManager.cancelReminder(outcome.original.id)
                outcome.spawned?.let { reminderManager.scheduleReminder(it) }
                TaskWidget().updateAll(context)
                outcome.spawned
            }
            CompletionOutcome.AlreadyCompleted, CompletionOutcome.NotFound -> null
        }
    }

    /**
     * Reverses [completeTask]: marks [taskId] active again and removes the next
     * occurrence it spawned, if any. See [TaskDao.uncompleteTaskAndRemoveSpawned]
     * for why this re-reads the task and its spawned occurrence fresh rather
     * than trusting a caller-supplied snapshot or an in-memory record of what
     * was spawned. Returns the removed occurrence, or null if there was none
     * (or the task was already active).
     */
    suspend fun uncompleteTask(taskId: Long): Task? {
        return when (val outcome = taskDao.uncompleteTaskAndRemoveSpawned(taskId)) {
            is UncompleteOutcome.Uncompleted -> {
                outcome.removedSpawn?.let { reminderManager.cancelReminder(it.id) }
                val reactivated = outcome.original.copy(isCompleted = false)
                if (reactivated.dueDate != null) {
                    reminderManager.scheduleReminder(reactivated)
                }
                TaskWidget().updateAll(context)
                outcome.removedSpawn
            }
            UncompleteOutcome.AlreadyActive, UncompleteOutcome.NotFound -> null
        }
    }
}
