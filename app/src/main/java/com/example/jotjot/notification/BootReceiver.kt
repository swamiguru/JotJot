package com.example.jotjot.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.jotjot.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val database = AppDatabase.getDatabase(context)
            val taskDao = database.taskDao()
            val reminderManager = ReminderManager(context)

            CoroutineScope(Dispatchers.IO).launch {
                val tasksToReschedule = taskDao.getActiveTasksWithReminders()
                tasksToReschedule.forEach { task ->
                    if (task.dueDate != null && task.dueDate > System.currentTimeMillis()) {
                        reminderManager.scheduleReminder(task)
                    }
                }
            }
        }
    }
}
