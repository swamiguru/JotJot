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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jotjot.data.Task
import java.text.SimpleDateFormat
import java.util.*

import com.example.jotjot.data.Priority
import com.example.jotjot.data.Recurrence
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import com.example.jotjot.ui.theme.JotJotTheme
import android.content.res.Configuration

@Composable
fun TaskScreen(
    viewModel: TaskViewModel = viewModel(),
    initialTaskIdToEdit: Long = -1L
) {
    val tasks by viewModel.allTasks.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    TaskContent(
        tasks = tasks,
        sortDirection = sortDirection,
        sortOrder = sortOrder,
        searchQuery = searchQuery,
        initialTaskIdToEdit = initialTaskIdToEdit,
        onSearchQueryChange = viewModel::setSearchQuery,
        onToggleSortDirection = viewModel::toggleSortDirection,
        onSortOrderChange = viewModel::setSortOrder,
        onToggleTaskCompletion = viewModel::toggleTaskCompletion,
        onDeleteTask = viewModel::deleteTask,
        onAddTask = viewModel::addTask,
        onUpdateTask = viewModel::updateTask
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskContent(
    tasks: List<Task>,
    sortDirection: SortDirection,
    sortOrder: SortOrder,
    searchQuery: String,
    initialTaskIdToEdit: Long = -1L,
    onSearchQueryChange: (String) -> Unit,
    onToggleSortDirection: () -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onToggleTaskCompletion: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onAddTask: (String, String?, Long?, Priority, Recurrence) -> Unit,
    onUpdateTask: (Task, String, String?, Long?, Priority, Recurrence) -> Unit
) {
    val activeTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }
    
    var showDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var taskTitle by remember { mutableStateOf("") }
    var taskNotes by remember { mutableStateOf("") }
    var taskPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var taskRecurrence by remember { mutableStateOf(Recurrence.NONE) }
    var selectedDateTime by remember { mutableStateOf<Calendar?>(null) }
    var isCompletedExpanded by remember { mutableStateOf(false) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(initialTaskIdToEdit, tasks) {
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
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { onSearchQueryChange(it) },
                            placeholder = { Text("Search tasks...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text("JotJot", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { 
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) onSearchQueryChange("")
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearchActive) "Close Search" else "Search"
                        )
                    }
                    IconButton(onClick = { onToggleSortDirection() }) {
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
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        onSortOrderChange(order)
                                        showSortMenu = false
                                    },
                                    trailingIcon = if (order == sortOrder) {
                                        { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                    } else null
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                editingTask = null
                taskTitle = ""
                taskNotes = ""
                taskPriority = Priority.MEDIUM
                taskRecurrence = Recurrence.NONE
                selectedDateTime = null
                showDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (activeTasks.isEmpty() && completedTasks.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tasks yet. Tap + to add one!", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                items(activeTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggleCompletion = { onToggleTaskCompletion(task) },
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
                            onClick = { isCompletedExpanded = !isCompletedExpanded },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Transparent
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Completed (${completedTasks.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (isCompletedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    if (isCompletedExpanded) {
                        items(completedTasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onToggleCompletion = { onToggleTaskCompletion(task) },
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
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Task") },
                text = { Text("Are you sure you want to delete this task?") },
                confirmButton = {
                    TextButton(onClick = {
                        taskToDelete?.let { onDeleteTask(it) }
                        showDeleteConfirmation = false
                        taskToDelete = null
                    }) {
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
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDateTime?.timeInMillis ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedDate = Calendar.getInstance().apply {
                            timeInMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        }
                        selectedDateTime = selectedDate
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
            val currentTime = Calendar.getInstance()
            val hasTime = selectedDateTime?.get(Calendar.MILLISECOND) == 1
            val timePickerState = rememberTimePickerState(
                initialHour = if (hasTime) selectedDateTime?.get(Calendar.HOUR_OF_DAY) ?: currentTime.get(Calendar.HOUR_OF_DAY) else currentTime.get(Calendar.HOUR_OF_DAY),
                initialMinute = if (hasTime) selectedDateTime?.get(Calendar.MINUTE) ?: currentTime.get(Calendar.MINUTE) else currentTime.get(Calendar.MINUTE)
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedDateTime?.apply {
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 1) // 1ms flags "has time"
                        }
                        showTimePicker = false
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedDateTime?.apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0) // 0ms flags "no time"
                        }
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
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            
            Dialog(
                onDismissRequest = { showDialog = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showDialog = false },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(if (isLandscape) 0.85f else 0.95f)
                            .imePadding()
                            .clickable(enabled = false) { }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = if (editingTask == null) "New Task" else "Edit Task",
                                style = MaterialTheme.typography.headlineMedium
                            )

                            if (isLandscape) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    OutlinedTextField(
                                        value = taskTitle,
                                        onValueChange = { taskTitle = it },
                                        placeholder = { Text("Title") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = MaterialTheme.shapes.large
                                    )

                                    OutlinedTextField(
                                        value = taskNotes,
                                        onValueChange = { taskNotes = it },
                                        placeholder = { Text("Notes") },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 2,
                                        shape = MaterialTheme.shapes.large
                                    )
                                }
                            } else {
                                OutlinedTextField(
                                    value = taskTitle,
                                    onValueChange = { taskTitle = it },
                                    placeholder = { Text("Title") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.large
                                )

                                OutlinedTextField(
                                    value = taskNotes,
                                    onValueChange = { taskNotes = it },
                                    placeholder = { Text("Notes") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    shape = MaterialTheme.shapes.large
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Priority", style = MaterialTheme.typography.titleMedium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Priority.entries.forEach { priority ->
                                    FilterChip(
                                        selected = taskPriority == priority,
                                        onClick = { taskPriority = priority },
                                        label = { 
                                            Text(
                                                text = priority.name.lowercase().replaceFirstChar { it.uppercase() },
                                                style = MaterialTheme.typography.bodyLarge
                                            ) 
                                        },
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

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Repeat", style = MaterialTheme.typography.titleMedium)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Recurrence.entries.forEach { recurrence ->
                                    FilterChip(
                                        selected = taskRecurrence == recurrence,
                                        onClick = { taskRecurrence = recurrence },
                                        label = { 
                                            Text(
                                                text = if (recurrence == Recurrence.NONE) "None" else recurrence.name.lowercase().replaceFirstChar { it.uppercase() },
                                                style = MaterialTheme.typography.bodyLarge
                                            ) 
                                        }
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDatePicker = true }
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Due Date",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )

                                selectedDateTime?.let {
                                    val hasTime = it.get(Calendar.MILLISECOND) == 1
                                    val pattern = if (hasTime) "MMM dd, HH:mm" else "MMM dd"
                                    Text(
                                        text = SimpleDateFormat(pattern, Locale.getDefault()).format(it.time),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    IconButton(
                                        onClick = { selectedDateTime = null },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear, 
                                            contentDescription = "Clear Date",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (editingTask != null) {
                                val isTaskCompleted = editingTask?.isCompleted == true
                                Button(
                                    onClick = {
                                        editingTask?.let { onToggleTaskCompletion(it) }
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
                                TextButton(
                                    onClick = { showDialog = false },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (taskTitle.isNotBlank()) {
                                            val currentTask = editingTask
                                            if (currentTask == null) {
                                                onAddTask(taskTitle, taskNotes.ifBlank { null }, selectedDateTime?.timeInMillis, taskPriority, taskRecurrence)
                                            } else {
                                                onUpdateTask(currentTask, taskTitle, taskNotes.ifBlank { null }, selectedDateTime?.timeInMillis, taskPriority, taskRecurrence)
                                            }
                                            taskTitle = ""
                                            taskNotes = ""
                                            taskPriority = Priority.MEDIUM
                                            taskRecurrence = Recurrence.NONE
                                            selectedDateTime = null
                                            editingTask = null
                                            showDialog = false
                                        }
                                    },
                                    modifier = Modifier.height(48.dp),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Save", style = MaterialTheme.typography.labelLarge)
                                }
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
        shape = MaterialTheme.shapes.large,
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
                            val cal = Calendar.getInstance().apply { timeInMillis = dueDate }
                            val hasTime = cal.get(Calendar.MILLISECOND) == 1
                            val pattern = if (hasTime) "MMM dd, HH:mm" else "MMM dd"
                            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
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
                RadioButton(
                    selected = task.isCompleted,
                    onClick = { onToggleCompletion() }
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

@Preview(showBackground = true)
@Composable
fun TaskScreenPreview() {
    JotJotTheme {
        TaskContent(
            tasks = listOf(
                Task(id = 1, title = "Finish Project", notes = "Due by EOD", priority = Priority.HIGH),
                Task(id = 2, title = "Grocery shopping", notes = "Milk, Eggs, Bread", priority = Priority.MEDIUM),
                Task(id = 3, title = "Call mom", priority = Priority.LOW, isCompleted = true)
            ),
            sortDirection = SortDirection.DESCENDING,
            sortOrder = SortOrder.CREATION_DATE,
            searchQuery = "",
            onSearchQueryChange = {},
            onToggleSortDirection = {},
            onSortOrderChange = {},
            onToggleTaskCompletion = {},
            onDeleteTask = {},
            onAddTask = { _, _, _, _, _ -> },
            onUpdateTask = { _, _, _, _, _, _ -> }
        )
    }
}
