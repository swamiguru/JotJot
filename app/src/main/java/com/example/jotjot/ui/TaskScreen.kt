package com.example.jotjot.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jotjot.data.Task
import java.text.SimpleDateFormat
import java.util.*

import com.example.jotjot.data.Priority
import com.example.jotjot.data.Recurrence
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel = viewModel(),
    initialTaskIdToEdit: Long = -1L
) {
    val tasks by viewModel.allTasks.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    
    val activeTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }
    
    var showDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var taskTitle by remember { mutableStateOf("") }
    var taskNotes by remember { mutableStateOf("") }
    var taskPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var taskRecurrence by remember { mutableStateOf(Recurrence.NONE) }
    var selectedDateTime by remember { mutableStateOf<Calendar?>(null) }
    var isCompletedExpanded by remember { mutableStateOf(false) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var datePickerState = rememberDatePickerState()
    var timePickerState = rememberTimePickerState()

    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(initialTaskIdToEdit) {
        if (initialTaskIdToEdit != -1L) {
            val task = tasks.find { it.id == initialTaskIdToEdit }
            if (task != null) {
                editingTask = task
                taskTitle = task.title
                taskNotes = task.notes ?: ""
                taskPriority = task.priority
                taskRecurrence = task.recurrence
                selectedDateTime = task.dueDate?.let {
                    Calendar.getInstance().apply { timeInMillis = it }
                }
                showDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JotJot", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.toggleSortDirection() }) {
                        Icon(
                            imageVector = if (sortDirection == SortDirection.ASCENDING) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Sort Direction"
                        )
                    }
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.List, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        viewModel.setSortOrder(order)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == order) Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    taskTitle = ""
                    taskNotes = ""
                    taskPriority = Priority.MEDIUM
                    taskRecurrence = Recurrence.NONE
                    selectedDateTime = null
                    editingTask = null
                    showDialog = true 
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (activeTasks.isNotEmpty()) {
                item {
                    Text(
                        "Active Tasks",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }
            }

            if (activeTasks.isEmpty() && completedTasks.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tasks yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(activeTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                    onClick = {
                        editingTask = task
                        taskTitle = task.title
                        taskNotes = task.notes ?: ""
                        taskPriority = task.priority
                        taskRecurrence = task.recurrence
                        selectedDateTime = task.dueDate?.let {
                            Calendar.getInstance().apply { timeInMillis = it }
                        }
                        showDialog = true
                    },
                    onDelete = {
                        taskToDelete = task
                        showDeleteConfirmation = true
                    }
                )
            }

            if (completedTasks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { isCompletedExpanded = !isCompletedExpanded }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Completed Tasks",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = completedTasks.size.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isCompletedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                if (isCompletedExpanded) {
                    items(completedTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                            onClick = {
                                editingTask = task
                                taskTitle = task.title
                                taskNotes = task.notes ?: ""
                                taskPriority = task.priority
                                taskRecurrence = task.recurrence
                                selectedDateTime = task.dueDate?.let {
                                    Calendar.getInstance().apply { timeInMillis = it }
                                }
                                showDialog = true
                            },
                            onDelete = {
                                taskToDelete = task
                                showDeleteConfirmation = true
                            }
                        )
                    }
                }
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Task") },
                text = { Text("Are you sure you want to delete this task?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            taskToDelete?.let { viewModel.deleteTask(it) }
                            showDeleteConfirmation = false
                            taskToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedDate = Calendar.getInstance().apply {
                            timeInMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        }
                        val current = selectedDateTime ?: Calendar.getInstance()
                        selectedDateTime = Calendar.getInstance().apply {
                            set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
                            set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))
                            if (selectedDateTime != null) {
                                set(Calendar.HOUR_OF_DAY, current.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, current.get(Calendar.MINUTE))
                            } else {
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                            }
                        }
                        showDatePicker = false
                        showTimePicker = true
                    }) { Text("Next") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedDateTime = (selectedDateTime ?: Calendar.getInstance()).apply {
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }
                        showTimePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        // If canceled, we keep the date but don't set a specific time (or keep old time)
                        showTimePicker = false
                    }) { Text("No Time") }
                },
                title = { Text("Select Time") },
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TimePicker(state = timePickerState)
                    }
                }
            )
        }

        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (editingTask == null) "New Task" else "Edit Task",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = taskNotes,
                            onValueChange = { taskNotes = it },
                            label = { Text("Notes (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )

                        Text("Priority", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Priority.values().forEach { priority ->
                                FilterChip(
                                    selected = taskPriority == priority,
                                    onClick = { taskPriority = priority },
                                    label = { Text(priority.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when (priority) {
                                            Priority.HIGH -> MaterialTheme.colorScheme.errorContainer
                                            Priority.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
                                            Priority.LOW -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        selectedLabelColor = when (priority) {
                                            Priority.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                                            Priority.MEDIUM -> MaterialTheme.colorScheme.onSecondaryContainer
                                            Priority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                )
                            }
                        }

                        Text("Repeat", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Recurrence.values().forEach { recurrence ->
                                FilterChip(
                                    selected = taskRecurrence == recurrence,
                                    onClick = { taskRecurrence = recurrence },
                                    label = { Text(recurrence.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { showDatePicker = true }
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    selectedDateTime?.let {
                                        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(it.time)
                                    } ?: "Set Due Date"
                                )
                            }
                            if (selectedDateTime != null) {
                                IconButton(onClick = { selectedDateTime = null }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Date")
                                }
                            }
                        }

                        if (editingTask != null) {
                            val isTaskCompleted = editingTask?.isCompleted == true
                            Button(
                                onClick = {
                                    editingTask?.let { viewModel.toggleTaskCompletion(it) }
                                    showDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTaskCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    if (isTaskCompleted) Icons.Default.Refresh else Icons.Default.Check,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (isTaskCompleted) "Mark as Active" else "Mark as Completed")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Cancel")
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    if (taskTitle.isNotBlank()) {
                                        val currentTask = editingTask
                                        if (currentTask == null) {
                                            viewModel.addTask(taskTitle, taskNotes.ifBlank { null }, selectedDateTime?.timeInMillis, taskPriority, taskRecurrence)
                                        } else {
                                            viewModel.updateTask(currentTask, taskTitle, taskNotes.ifBlank { null }, selectedDateTime?.timeInMillis, taskPriority, taskRecurrence)
                                        }
                                        taskTitle = ""
                                        taskNotes = ""
                                        taskPriority = Priority.MEDIUM
                                        taskRecurrence = Recurrence.NONE
                                        selectedDateTime = null
                                        editingTask = null
                                        showDialog = false
                                    }
                                }
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task, 
    onToggleCompletion: () -> Unit, 
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val alpha by animateFloatAsState(if (task.isCompleted) 0.6f else 1f, label = "taskAlpha")
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clickable { onClick() },
        color = if (task.isCompleted) 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (task.isCompleted) 0.dp else 1.dp
    ) {
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            ),
            headlineContent = { 
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!task.notes.isNullOrBlank()) {
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        // Priority Badge
                        if (!task.isCompleted) {
                            Surface(
                                color = when (task.priority) {
                                    Priority.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                                    Priority.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                                    Priority.LOW -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                },
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = task.priority.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = when (task.priority) {
                                        Priority.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                                        Priority.MEDIUM -> MaterialTheme.colorScheme.onSecondaryContainer
                                        Priority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        // Due Date
                        task.dueDate?.let { dueDate ->
                            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = sdf.format(Date(dueDate)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Recurrence Tag
                        if (task.recurrence != Recurrence.NONE) {
                            Surface(
                                color = if (task.isCompleted) 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else 
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (task.isCompleted) 
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        else 
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = task.recurrence.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (task.isCompleted) 
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        else 
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            },
            leadingContent = {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleCompletion() }
                )
            },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }
}
