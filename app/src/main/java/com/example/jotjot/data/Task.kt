package com.example.jotjot.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority {
    LOW, MEDIUM, HIGH
}

enum class Recurrence {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val recurrence: Recurrence = Recurrence.NONE,
    // Id of the task this row was spawned from on completion (recurring tasks
    // only), or null otherwise. Durable link -- stored in the DB rather than
    // kept only in memory -- so un-completing the original can reliably find
    // and remove the occurrence it spawned, even after the app restarts or
    // the undo snackbar's window has long passed.
    val spawnedFromId: Long? = null
)
