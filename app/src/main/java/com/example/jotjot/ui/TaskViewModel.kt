package com.example.jotjot.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jotjot.data.AppDatabase
import com.example.jotjot.data.Task
import com.example.jotjot.data.TaskRepository
import com.example.jotjot.notification.ReminderManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

import com.example.jotjot.data.Priority
import com.example.jotjot.data.Recurrence
import com.example.jotjot.widget.TaskWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Calendar
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
        repository = TaskRepository(taskDao, reminderManager)
        
        allTasks = combine(repository.allTasks, _sortOrder, _sortDirection, _searchQuery) { tasks, sortOrder, sortDirection, query ->
            val filteredTasks = if (query.isBlank()) {
                tasks
            } else {
                tasks.filter { 
                    it.title.contains(query, ignoreCase = true) || 
                    (it.notes?.contains(query, ignoreCase = true) ?: false)
                }
            }

            val sortedTasks = when (sortOrder) {
                SortOrder.CREATION_DATE -> filteredTasks.sortedBy { it.createdAt }
                SortOrder.DUE_DATE -> filteredTasks.sortedWith(compareBy<Task> { it.dueDate == null }.thenBy { it.dueDate })
                SortOrder.PRIORITY -> filteredTasks.sortedBy { it.priority }
            }
            
            if (sortDirection == SortDirection.DESCENDING) {
                sortedTasks.reversed()
            } else {
                sortedTasks
            }
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
            TaskWidget().updateAll(getApplication())
        }
    }

    fun updateTask(task: Task, newTitle: String, newNotes: String?, newDueDate: Long?, newPriority: Priority, newRecurrence: Recurrence) {
        viewModelScope.launch {
            repository.update(task.copy(title = newTitle, notes = newNotes, dueDate = newDueDate, priority = newPriority, recurrence = newRecurrence))
            TaskWidget().updateAll(getApplication())
        }
    }

    fun toggleTaskCompletion(task: Task, isCompleted: Boolean? = null) {
        viewModelScope.launch {
            val targetStatus = isCompleted ?: !task.isCompleted
            if (targetStatus && !task.isCompleted && task.recurrence != Recurrence.NONE && task.dueDate != null) {
                // If a recurring task is completed, create the next instance
                val nextDueDate = calculateNextDueDate(task.dueDate, task.recurrence)
                repository.insert(task.copy(id = 0, isCompleted = false, dueDate = nextDueDate, createdAt = System.currentTimeMillis()))
            }
            repository.update(task.copy(isCompleted = targetStatus))
            TaskWidget().updateAll(getApplication())
        }
    }

    private fun calculateNextDueDate(currentDueDate: Long, recurrence: Recurrence): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentDueDate }
        when (recurrence) {
            Recurrence.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            Recurrence.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            Recurrence.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            Recurrence.YEARLY -> calendar.add(Calendar.YEAR, 1)
            Recurrence.NONE -> {}
        }
        return calendar.timeInMillis
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
            TaskWidget().updateAll(getApplication())
        }
    }
}
