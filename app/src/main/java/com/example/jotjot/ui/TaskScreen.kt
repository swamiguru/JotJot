package com.example.jotjot.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jotjot.data.Task
import com.example.jotjot.data.Priority
import com.example.jotjot.data.Recurrence
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random
import kotlin.math.sin

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.tooling.preview.Preview
import com.example.jotjot.ui.theme.JotJotTheme
import android.content.res.Configuration

@Composable
fun TaskScreen(
    viewModel: TaskViewModel = viewModel(),
    initialTaskIdToEdit: Long = -1L,
    showAddTaskDialog: Boolean = false
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
        showAddTaskDialog = showAddTaskDialog,
        onSearchQueryChange = viewModel::setSearchQuery,
        onToggleSortDirection = viewModel::toggleSortDirection,
        onSortOrderChange = viewModel::setSortOrder,
        onToggleTaskCompletion = { task, status -> viewModel.toggleTaskCompletion(task, status) },
        onDeleteTask = viewModel::deleteTask,
        onAddTask = viewModel::addTask,
        onUpdateTask = viewModel::updateTask
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskContent(
    tasks: List<Task>,
    sortDirection: SortDirection,
    sortOrder: SortOrder,
    searchQuery: String,
    initialTaskIdToEdit: Long = -1L,
    showAddTaskDialog: Boolean = false,
    onSearchQueryChange: (String) -> Unit,
    onToggleSortDirection: () -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onToggleTaskCompletion: (Task, Boolean?) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onAddTask: (String, String?, Long?, Priority, Recurrence) -> Unit,
    onUpdateTask: (Task, String, String?, Long?, Priority, Recurrence) -> Unit
) {
    val activeTasks = remember(tasks) { tasks.filter { !it.isCompleted } }
    val completedTasks = remember(tasks) { tasks.filter { it.isCompleted } }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    
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
    var showCelebration by remember { mutableStateOf(false) }

    val handleToggleCompletion = { task: Task, completed: Boolean? ->
        val targetStatus = completed ?: !task.isCompleted
        if (targetStatus && !task.isCompleted && task.priority == Priority.HIGH) {
            showCelebration = true
        }
        onToggleTaskCompletion(task, completed)
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

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

    LaunchedEffect(showAddTaskDialog) {
        if (showAddTaskDialog) {
            editingTask = null
            taskTitle = ""
            taskNotes = ""
            taskPriority = Priority.MEDIUM
            taskRecurrence = Recurrence.NONE
            selectedDateTime = null
            showDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = isSearchActive,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(300, delayMillis = 150)) + 
                             scaleIn(initialScale = 0.92f, animationSpec = tween(300, delayMillis = 150)))
                                .togetherWith(fadeOut(animationSpec = tween(150)))
                        },
                        label = "searchTransition"
                    ) { active ->
                        if (active) {
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
                            Text(
                                text = "JotJot",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Sort")
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
                if (activeTasks.isNotEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Active Tasks",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                items(activeTasks, key = { it.id }) { task ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            when (it) {
                                SwipeToDismissBoxValue.StartToEnd -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            handleToggleCompletion(task, true)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Task completed",
                                                    actionLabel = "UNDO",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    onToggleTaskCompletion(task, false)
                                                }
                                            }
                                    true
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onDeleteTask(task)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Task deleted",
                                                    actionLabel = "UNDO",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    onAddTask(task.title, task.notes, task.dueDate, task.priority, task.recurrence)
                                                }
                                            }
                                    true
                                }
                                else -> false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = { SwipeBackground(dismissState) },
                        modifier = Modifier.animateItem(
                            placementSpec = tween(durationMillis = 600)
                        )
                    ) {
                        TaskCard(
                            task = task,
                            onToggleCompletion = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                handleToggleCompletion(task, true)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Task completed",
                                        actionLabel = "UNDO",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onToggleTaskCompletion(task, false)
                                    }
                                }
                            },
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
                            }
                        )
                    }
                }
                
                if (completedTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = CircleShape,
                            modifier = Modifier
                                .clickable { isCompletedExpanded = !isCompletedExpanded }
                                .padding(vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Completed",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                ) {
                                    Text(
                                        text = completedTasks.size.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                val rotation by animateFloatAsState(
                                    targetValue = if (isCompletedExpanded) 180f else 0f,
                                    label = "rotation"
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp).rotate(rotation),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                    
                    if (isCompletedExpanded) {
                        items(completedTasks, key = { it.id }) { task ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    when (it) {
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            handleToggleCompletion(task, false)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Task marked as active",
                                                    actionLabel = "UNDO",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    onToggleTaskCompletion(task, true)
                                                }
                                            }
                                            true
                                        }
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onDeleteTask(task)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Task deleted",
                                                    actionLabel = "UNDO",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    onAddTask(task.title, task.notes, task.dueDate, task.priority, task.recurrence)
                                                }
                                            }
                                            true
                                        }
                                        else -> false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = { SwipeBackground(dismissState) },
                                modifier = Modifier.animateItem(
                                    placementSpec = tween(durationMillis = 600)
                                )
                            ) {
                                TaskCard(
                                    task = task,
                                    onToggleCompletion = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        handleToggleCompletion(task, false)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Task marked as active",
                                                actionLabel = "UNDO",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                onToggleTaskCompletion(task, true)
                                            }
                                        }
                                    },
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
                                    }
                                )
                            }
                        }
                    }
                }
            }
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

                            if (isLandscape) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    OutlinedTextField(
                                        value = taskTitle,
                                        onValueChange = { taskTitle = it },
                                        placeholder = { Text("What would you like to do?") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = MaterialTheme.shapes.large
                                    )

                                    OutlinedTextField(
                                        value = taskNotes,
                                        onValueChange = { taskNotes = it },
                                        placeholder = { Text("Description") },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 2,
                                        shape = MaterialTheme.shapes.large
                                    )
                                }
                            } else {
                                OutlinedTextField(
                                    value = taskTitle,
                                    onValueChange = { taskTitle = it },
                                    placeholder = { Text("What would you like to do?") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.large
                                )

                                OutlinedTextField(
                                    value = taskNotes,
                                    onValueChange = { taskNotes = it },
                                    placeholder = { Text("Description") },
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
                                    imageVector = Icons.Default.Notifications,
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
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        editingTask?.let { handleToggleCompletion(it, null) }
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

        CelebrationOverlay(
            visible = showCelebration,
            onDismiss = { showCelebration = false }
        )
    }
}
}

@Composable
private fun CelebrationOverlay(visible: Boolean, onDismiss: () -> Unit) {
    if (!visible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        ConfettiEffect()
    }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000)
        onDismiss()
    }
}

@Composable
private fun ConfettiEffect() {
    val colors = listOf(
        Color(0xFFFF5252), Color(0xFFFFEB3B), Color(0xFF448AFF),
        Color(0xFF69F0AE), Color(0xFFFF4081), Color(0xFF7C4DFF),
        Color(0xFF18FFFF), Color(0xFFE040FB)
    )
    
    val particles = remember {
        List(150) {
            ConfettiParticle(color = colors.random())
        }
    }

    val frameClock = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameClock.longValue = it }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        frameClock.longValue // Force redraw on every frame
        particles.forEach { p ->
            p.update(size.width, size.height)
            rotate(p.rotation, pivot = Offset(p.x, p.y)) {
                // Realistic flutter effect by oscillating the width
                val flutter = sin(p.sideWaysMotion).coerceAtLeast(0.1f)
                drawRect(
                    color = p.color,
                    topLeft = Offset(p.x, p.y),
                    size = androidx.compose.ui.geometry.Size(p.width * flutter, p.height),
                    alpha = p.alpha
                )
            }
        }
    }
}

private class ConfettiParticle(val color: Color) {
    var x = 0f
    var y = 0f
    var width = 0f
    var height = 0f
    var vx = 0f
    var vy = 0f
    var rotation = 0f
    var rotationSpeed = 0f
    var alpha = 1f
    var sideWaysMotion = 0f
    var sideWaysSpeed = 0f
    var isInitialized = false

    fun reset(screenWidth: Float, screenHeight: Float, init: Boolean = false) {
        x = Random.nextFloat() * screenWidth
        y = if (init) (Random.nextFloat() * 2f - 1.5f) * screenHeight else -50f
        width = Random.nextFloat() * 40f + 30f
        height = width * 0.6f
        vx = Random.nextFloat() * 8f - 4f
        vy = Random.nextFloat() * 12f + 8f
        rotation = Random.nextFloat() * 360f
        rotationSpeed = Random.nextFloat() * 15f - 7.5f
        sideWaysMotion = Random.nextFloat() * 2f * Math.PI.toFloat()
        sideWaysSpeed = Random.nextFloat() * 0.1f + 0.05f
        alpha = 1f
        isInitialized = true
    }

    fun update(screenWidth: Float, screenHeight: Float) {
        if (!isInitialized) {
            reset(screenWidth, screenHeight, init = true)
        }

        sideWaysMotion += sideWaysSpeed
        x += vx + sin(sideWaysMotion) * 2f
        y += vy
        rotation += rotationSpeed
        
        // Simulating some drag and gravity
        vy += 0.05f // gravity
        
        if (y > screenHeight) {
            reset(screenWidth, screenHeight)
        }
        
        // Horizontal wrap
        if (x < -50f) x = screenWidth + 50f
        if (x > screenWidth + 50f) x = -50f
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeBackground(dismissState: SwipeToDismissBoxState) {
    val color = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
        else -> Color.Transparent
    }
    val alignment = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    val icon = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Check
        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
        else -> Icons.Default.Delete
    }
    val iconTint = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onPrimaryContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onErrorContainer
        else -> Color.White
    }
    val scale by animateFloatAsState(
        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "iconScale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(color, shape = CircleShape)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp).alpha(scale),
            tint = iconTint
        )
    }
}

@Composable
fun TaskCard(
    task: Task, 
    onToggleCompletion: () -> Unit, 
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.6f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "taskAlpha"
    )
    
    val containerColor by animateColorAsState(
        targetValue = if (task.isCompleted) 
            MaterialTheme.colorScheme.surfaceContainer
        else 
            MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(durationMillis = 500),
        label = "containerColor"
    )
    
    val tonalElevation by animateDpAsState(
        targetValue = if (task.isCompleted) 0.dp else 1.dp,
        label = "tonalElevation"
    )

    val formattedDate = remember(task.dueDate) {
        task.dueDate?.let { dueDate ->
            val cal = Calendar.getInstance().apply { timeInMillis = dueDate }
            val hasTime = cal.get(Calendar.MILLISECOND) == 1
            val pattern = if (hasTime) "MMM dd, HH:mm" else "MMM dd"
            SimpleDateFormat(pattern, Locale.getDefault()).format(Date(dueDate))
        }
    }

    val isOverdue = remember(task.dueDate, task.isCompleted) {
        task.dueDate?.let { it < System.currentTimeMillis() && !task.isCompleted } ?: false
    }

    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val borderStroke = remember(task.isCompleted, outlineVariant) {
        if (task.isCompleted) null else androidx.compose.foundation.BorderStroke(1.dp, outlineVariant.copy(alpha = 0.2f))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .clickable { onClick() },
        color = containerColor,
        shape = CircleShape,
        tonalElevation = tonalElevation,
        border = borderStroke
    ) {
        ListItem(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            ),
            headlineContent = {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            supportingContent = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (!task.notes.isNullOrBlank()) {
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Priority Badge
                        Surface(
                            color = when (task.priority) {
                                Priority.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                                Priority.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                                Priority.LOW -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            },
                            shape = CircleShape
                        ) {
                            Text(
                                text = task.priority.name,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = when (task.priority) {
                                    Priority.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                                    Priority.MEDIUM -> MaterialTheme.colorScheme.onSecondaryContainer
                                    Priority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        // Due Date
                        formattedDate?.let { dateString ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = CircleShape,
                                border = if (isOverdue)
                                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                else null
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isOverdue)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = dateString,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOverdue)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Recurrence Tag
                        if (task.recurrence != Recurrence.NONE) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = CircleShape
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = task.recurrence.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
            onToggleTaskCompletion = { _, _ -> },
            onDeleteTask = {},
            onAddTask = { _, _, _, _, _ -> },
            onUpdateTask = { _, _, _, _, _, _ -> }
        )
    }
}
