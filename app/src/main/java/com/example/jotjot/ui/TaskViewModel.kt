package com.example.jotjot.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jotjot.data.AppDatabase
import com.example.jotjot.data.Task
import com.example.jotjot.data.TaskRepository
import com.example.jotjot.notification.ReminderManager
import kotlinx.coroutines.launch

import com.example.jotjot.data.Priority
import com.example.jotjot.data.Recurrence
import com.example.jotjot.widget.TaskWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class SortOrder {
    CREATION_DATE, DUE_DATE, PRIORITY
}

enum class SortDirection {
    ASCENDING, DESCENDING
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    private val _sortOrder = MutableStateFlow(SortOrder.CREATION_DATE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _sortDirection = MutableStateFlow(SortDirection.DESCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val allTasks: StateFlow<List<Task>>

    init {
        val taskDao = AppDatabase.getDatabase(application).taskDao()
        val reminderManager = ReminderManager(application)
        repository = TaskRepository(taskDao, reminderManager, application)

        allTasks = combine(repository.allTasks, _sortOrder, _sortDirection, _searchQuery) { tasks, sortOrder, sortDirection, query ->
            TaskListLogic.filterAndSort(tasks, sortOrder, sortDirection, query)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleSortDirection() {
        _sortDirection.value = if (_sortDirection.value == SortDirection.ASCENDING) {
            SortDirection.DESCENDING
        } else {
            SortDirection.ASCENDING
        }
    }

    fun addTask(title: String, notes: String? = null, dueDate: Long? = null, priority: Priority = Priority.MEDIUM, recurrence: Recurrence = Recurrence.NONE) {
        viewModelScope.launch {
            repository.insert(Task(title = title, notes = notes, dueDate = dueDate, priority = priority, recurrence = recurrence))
        }
    }

    fun updateTask(task: Task, newTitle: String, newNotes: String?, newDueDate: Long?, newPriority: Priority, newRecurrence: Recurrence) {
        viewModelScope.launch {
            repository.update(task.copy(title = newTitle, notes = newNotes, dueDate = newDueDate, priority = newPriority, recurrence = newRecurrence))
        }
    }

    // Remembers the next occurrence spawned when a recurring task was completed,
    // keyed by the completed task's id, so an undo can remove it and avoid leaving
    // a duplicate behind.
    private val spawnedByCompletedId = mutableMapOf<Long, Task>()

    fun toggleTaskCompletion(task: Task, isCompleted: Boolean? = null) {
        viewModelScope.launch {
            val targetStatus = isCompleted ?: !task.isCompleted
            if (targetStatus && !task.isCompleted) {
                // Completion (and any recurrence) is handled in one place by the repository.
                val spawned = repository.completeTask(task)
                if (spawned != null) {
                    spawnedByCompletedId[task.id] = spawned
                }
            } else {
                repository.update(task.copy(isCompleted = targetStatus))
                if (!targetStatus) {
                    // Undoing a completion: remove the next occurrence it created, if any.
                    spawnedByCompletedId.remove(task.id)?.let { repository.delete(it) }
                }
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }
}
