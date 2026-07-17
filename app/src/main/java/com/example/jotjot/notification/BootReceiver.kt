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

            // See NotificationReceiver for why goAsync() matters here: without it
            // the process can be killed partway through rescheduling, leaving some
            // reminders silently un-scheduled after a reboot.
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val tasksToReschedule = taskDao.getActiveTasksWithReminders()
                    tasksToReschedule.forEach { task ->
                        if (task.dueDate != null && task.dueDate > System.currentTimeMillis()) {
                            reminderManager.scheduleReminder(task)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
