package com.example.jotjot.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.jotjot.MainActivity
import com.example.jotjot.data.AppDatabase
import com.example.jotjot.data.Task
import com.example.jotjot.data.Priority
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class TaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)
        val taskDao = database.taskDao()
        
        val activeTasks = taskDao.getAllTasks().first()
            .filter { !it.isCompleted }
            .sortedBy { it.dueDate ?: Long.MAX_VALUE }

        provideContent {
            GlanceTheme {
                WidgetContent(context, activeTasks)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context, tasks: List<Task>) {
        val addIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_ADD_DIALOG, true)
        }
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                Text(
                    text = "JotJot",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(mainIntent))
                )
                
                Text(
                    text = "+ New",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .cornerRadius(8.dp)
                        .clickable(actionStartActivity(addIntent))
                )
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(mainIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "All caught up!",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Tap to add a task",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(tasks) { task ->
                        TaskItem(context, task)
                    }
                }
            }
        }
    }

    @Composable
    private fun TaskItem(context: Context, task: Task) {
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val taskIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(com.example.jotjot.notification.NotificationReceiver.EXTRA_TASK_ID, task.id)
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(12.dp)
                .padding(12.dp)
                .clickable(actionStartActivity(taskIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Indicator
            Box(
                modifier = GlanceModifier
                    .size(4.dp, 24.dp)
                    .background(getPriorityColor(task.priority))
                    .cornerRadius(2.dp)
            ) {}

            Spacer(modifier = GlanceModifier.width(12.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = task.title,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    ),
                    maxLines = 1
                )
                
                if (!task.notes.isNullOrBlank()) {
                    Text(
                        text = task.notes,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }
            }

            task.dueDate?.let { dueDate ->
                val calendar = Calendar.getInstance().apply { timeInMillis = dueDate }
                val hasTime = calendar.get(Calendar.MILLISECOND) == 1
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = dateFormat.format(Date(dueDate)),
                        style = TextStyle(
                            color = if (dueDate < System.currentTimeMillis()) 
                                GlanceTheme.colors.error 
                            else 
                                GlanceTheme.colors.onSecondaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (hasTime) {
                        Text(
                            text = timeFormat.format(Date(dueDate)),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSecondaryContainer,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun getPriorityColor(priority: Priority): ColorProvider {
        return when (priority) {
            Priority.HIGH -> GlanceTheme.colors.error
            Priority.MEDIUM -> GlanceTheme.colors.secondary
            Priority.LOW -> GlanceTheme.colors.outline
        }
    }
}
