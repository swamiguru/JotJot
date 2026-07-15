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
     * Marks [task] as completed. If it is a recurring task with a due date, the
     * next occurrence is created in the same step, with its due date advanced to
     * the next slot strictly after now (so an overdue task catches up instead of
     * immediately re-firing). Returns the newly created next occurrence (with its
     * database id) so callers can offer an undo, or null if nothing was spawned.
     *
     * This is the single source of truth for completion, shared by the UI and the
     * notification action, so recurrence behaves identically everywhere.
     */
    suspend fun completeTask(task: Task): Task? {
        var spawned: Task? = null
        if (!task.isCompleted && task.recurrence != Recurrence.NONE && task.dueDate != null) {
            val nextDue = RecurrenceCalculator.nextFutureDueDate(task.dueDate, task.recurrence)
            val next = task.copy(
                id = 0,
                isCompleted = false,
                dueDate = nextDue,
                createdAt = System.currentTimeMillis(),
            )
            val newId = taskDao.insertTask(next)
            spawned = next.copy(id = newId)
            reminderManager.scheduleReminder(spawned)
        }
        taskDao.updateTask(task.copy(isCompleted = true))
        reminderManager.cancelReminder(task.id)
        TaskWidget().updateAll(context)
        return spawned
    }
}
