package com.example.jotjot.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.jotjot.MainActivity
import com.example.jotjot.data.AppDatabase
import com.example.jotjot.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"
        val action = intent.action

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_COMPLETE -> {
                if (taskId != -1L) {
                    val database = AppDatabase.getDatabase(context)
                    val taskDao = database.taskDao()
                    val reminderManager = ReminderManager(context)
                    val repository = TaskRepository(taskDao, reminderManager, context)
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        val task = taskDao.getTaskById(taskId)
                        if (task != null) {
                            repository.update(task.copy(isCompleted = true))
                        }
                    }
                    notificationManager.cancel(taskId.toInt())
                }
            }
            ACTION_SNOOZE -> {
                if (taskId != -1L) {
                    val reminderManager = ReminderManager(context)
                    val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000) // 10 minutes
                    
                    val database = AppDatabase.getDatabase(context)
                    val taskDao = database.taskDao()
                    val repository = TaskRepository(taskDao, reminderManager, context)
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        val task = taskDao.getTaskById(taskId)
                        if (task != null) {
                            repository.update(task.copy(dueDate = snoozeTime))
                        }
                    }
                    notificationManager.cancel(taskId.toInt())
                }
            }
            else -> {
                showNotification(context, taskId, taskTitle)
            }
        }
    }

    private fun showNotification(context: Context, taskId: Long, title: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "task_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, taskId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_COMPLETE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context, taskId.toInt() + 1000, completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, taskId.toInt() + 2000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Task Due")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Mark Completed", completePendingIntent)
            .addAction(0, "Snooze (10m)", snoozePendingIntent)
            .build()

        notificationManager.notify(taskId.toInt(), notification)
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val ACTION_COMPLETE = "com.example.jotjot.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "com.example.jotjot.ACTION_SNOOZE"
    }
}
