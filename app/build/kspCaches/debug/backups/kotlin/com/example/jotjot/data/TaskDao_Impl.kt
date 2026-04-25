package com.example.jotjot.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TaskDao_Impl(
  __db: RoomDatabase,
) : TaskDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTask: EntityInsertAdapter<Task>

  private val __deleteAdapterOfTask: EntityDeleteOrUpdateAdapter<Task>

  private val __updateAdapterOfTask: EntityDeleteOrUpdateAdapter<Task>
  init {
    this.__db = __db
    this.__insertAdapterOfTask = object : EntityInsertAdapter<Task>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `tasks` (`id`,`title`,`notes`,`isCompleted`,`createdAt`,`dueDate`,`priority`,`recurrence`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Task) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        val _tmpNotes: String? = entity.notes
        if (_tmpNotes == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpNotes)
        }
        val _tmp: Int = if (entity.isCompleted) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindLong(5, entity.createdAt)
        val _tmpDueDate: Long? = entity.dueDate
        if (_tmpDueDate == null) {
          statement.bindNull(6)
        } else {
          statement.bindLong(6, _tmpDueDate)
        }
        statement.bindText(7, __Priority_enumToString(entity.priority))
        statement.bindText(8, __Recurrence_enumToString(entity.recurrence))
      }
    }
    this.__deleteAdapterOfTask = object : EntityDeleteOrUpdateAdapter<Task>() {
      protected override fun createQuery(): String = "DELETE FROM `tasks` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Task) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfTask = object : EntityDeleteOrUpdateAdapter<Task>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `tasks` SET `id` = ?,`title` = ?,`notes` = ?,`isCompleted` = ?,`createdAt` = ?,`dueDate` = ?,`priority` = ?,`recurrence` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Task) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        val _tmpNotes: String? = entity.notes
        if (_tmpNotes == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpNotes)
        }
        val _tmp: Int = if (entity.isCompleted) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindLong(5, entity.createdAt)
        val _tmpDueDate: Long? = entity.dueDate
        if (_tmpDueDate == null) {
          statement.bindNull(6)
        } else {
          statement.bindLong(6, _tmpDueDate)
        }
        statement.bindText(7, __Priority_enumToString(entity.priority))
        statement.bindText(8, __Recurrence_enumToString(entity.recurrence))
        statement.bindLong(9, entity.id)
      }
    }
  }

  public override suspend fun insertTask(task: Task): Long = performSuspending(__db, false, true) {
      _connection ->
    val _result: Long = __insertAdapterOfTask.insertAndReturnId(_connection, task)
    _result
  }

  public override suspend fun deleteTask(task: Task): Unit = performSuspending(__db, false, true) {
      _connection ->
    __deleteAdapterOfTask.handle(_connection, task)
  }

  public override suspend fun updateTask(task: Task): Unit = performSuspending(__db, false, true) {
      _connection ->
    __updateAdapterOfTask.handle(_connection, task)
  }

  public override fun getAllTasks(): Flow<List<Task>> {
    val _sql: String = "SELECT * FROM tasks ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _cursorIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _cursorIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _cursorIndexOfDueDate: Int = getColumnIndexOrThrow(_stmt, "dueDate")
        val _cursorIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _cursorIndexOfRecurrence: Int = getColumnIndexOrThrow(_stmt, "recurrence")
        val _result: MutableList<Task> = mutableListOf()
        while (_stmt.step()) {
          val _item: Task
          val _tmpId: Long
          _tmpId = _stmt.getLong(_cursorIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_cursorIndexOfTitle)
          val _tmpNotes: String?
          if (_stmt.isNull(_cursorIndexOfNotes)) {
            _tmpNotes = null
          } else {
            _tmpNotes = _stmt.getText(_cursorIndexOfNotes)
          }
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_cursorIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_cursorIndexOfCreatedAt)
          val _tmpDueDate: Long?
          if (_stmt.isNull(_cursorIndexOfDueDate)) {
            _tmpDueDate = null
          } else {
            _tmpDueDate = _stmt.getLong(_cursorIndexOfDueDate)
          }
          val _tmpPriority: Priority
          _tmpPriority = __Priority_stringToEnum(_stmt.getText(_cursorIndexOfPriority))
          val _tmpRecurrence: Recurrence
          _tmpRecurrence = __Recurrence_stringToEnum(_stmt.getText(_cursorIndexOfRecurrence))
          _item =
              Task(_tmpId,_tmpTitle,_tmpNotes,_tmpIsCompleted,_tmpCreatedAt,_tmpDueDate,_tmpPriority,_tmpRecurrence)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getActiveTasksWithReminders(): List<Task> {
    val _sql: String = "SELECT * FROM tasks WHERE isCompleted = 0 AND dueDate IS NOT NULL"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _cursorIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _cursorIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _cursorIndexOfDueDate: Int = getColumnIndexOrThrow(_stmt, "dueDate")
        val _cursorIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _cursorIndexOfRecurrence: Int = getColumnIndexOrThrow(_stmt, "recurrence")
        val _result: MutableList<Task> = mutableListOf()
        while (_stmt.step()) {
          val _item: Task
          val _tmpId: Long
          _tmpId = _stmt.getLong(_cursorIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_cursorIndexOfTitle)
          val _tmpNotes: String?
          if (_stmt.isNull(_cursorIndexOfNotes)) {
            _tmpNotes = null
          } else {
            _tmpNotes = _stmt.getText(_cursorIndexOfNotes)
          }
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_cursorIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_cursorIndexOfCreatedAt)
          val _tmpDueDate: Long?
          if (_stmt.isNull(_cursorIndexOfDueDate)) {
            _tmpDueDate = null
          } else {
            _tmpDueDate = _stmt.getLong(_cursorIndexOfDueDate)
          }
          val _tmpPriority: Priority
          _tmpPriority = __Priority_stringToEnum(_stmt.getText(_cursorIndexOfPriority))
          val _tmpRecurrence: Recurrence
          _tmpRecurrence = __Recurrence_stringToEnum(_stmt.getText(_cursorIndexOfRecurrence))
          _item =
              Task(_tmpId,_tmpTitle,_tmpNotes,_tmpIsCompleted,_tmpCreatedAt,_tmpDueDate,_tmpPriority,_tmpRecurrence)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getTaskById(id: Long): Task? {
    val _sql: String = "SELECT * FROM tasks WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _cursorIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _cursorIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _cursorIndexOfDueDate: Int = getColumnIndexOrThrow(_stmt, "dueDate")
        val _cursorIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _cursorIndexOfRecurrence: Int = getColumnIndexOrThrow(_stmt, "recurrence")
        val _result: Task?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_cursorIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_cursorIndexOfTitle)
          val _tmpNotes: String?
          if (_stmt.isNull(_cursorIndexOfNotes)) {
            _tmpNotes = null
          } else {
            _tmpNotes = _stmt.getText(_cursorIndexOfNotes)
          }
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_cursorIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_cursorIndexOfCreatedAt)
          val _tmpDueDate: Long?
          if (_stmt.isNull(_cursorIndexOfDueDate)) {
            _tmpDueDate = null
          } else {
            _tmpDueDate = _stmt.getLong(_cursorIndexOfDueDate)
          }
          val _tmpPriority: Priority
          _tmpPriority = __Priority_stringToEnum(_stmt.getText(_cursorIndexOfPriority))
          val _tmpRecurrence: Recurrence
          _tmpRecurrence = __Recurrence_stringToEnum(_stmt.getText(_cursorIndexOfRecurrence))
          _result =
              Task(_tmpId,_tmpTitle,_tmpNotes,_tmpIsCompleted,_tmpCreatedAt,_tmpDueDate,_tmpPriority,_tmpRecurrence)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __Priority_enumToString(_value: Priority): String = when (_value) {
    Priority.LOW -> "LOW"
    Priority.MEDIUM -> "MEDIUM"
    Priority.HIGH -> "HIGH"
  }

  private fun __Recurrence_enumToString(_value: Recurrence): String = when (_value) {
    Recurrence.NONE -> "NONE"
    Recurrence.DAILY -> "DAILY"
    Recurrence.WEEKLY -> "WEEKLY"
    Recurrence.MONTHLY -> "MONTHLY"
    Recurrence.YEARLY -> "YEARLY"
  }

  private fun __Priority_stringToEnum(_value: String): Priority = when (_value) {
    "LOW" -> Priority.LOW
    "MEDIUM" -> Priority.MEDIUM
    "HIGH" -> Priority.HIGH
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  private fun __Recurrence_stringToEnum(_value: String): Recurrence = when (_value) {
    "NONE" -> Recurrence.NONE
    "DAILY" -> Recurrence.DAILY
    "WEEKLY" -> Recurrence.WEEKLY
    "MONTHLY" -> Recurrence.MONTHLY
    "YEARLY" -> Recurrence.YEARLY
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
