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
}
