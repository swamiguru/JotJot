package com.example.jotjot

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.jotjot.ui.TaskScreen
import com.example.jotjot.ui.theme.JotJotTheme

import com.example.jotjot.notification.NotificationReceiver

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val taskId = intent.getLongExtra(NotificationReceiver.EXTRA_TASK_ID, -1L)
        val showAddTask = intent.getBooleanExtra(EXTRA_OPEN_ADD_DIALOG, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            JotJotTheme {
                TaskScreen(
                    initialTaskIdToEdit = taskId,
                    showAddTaskDialog = showAddTask
                )
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_ADD_DIALOG = "open_add_dialog"
    }
}
