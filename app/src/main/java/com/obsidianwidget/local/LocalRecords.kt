package com.obsidianwidget.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** App-owned records: independent of legacy Obsidian vault files. */
@Entity(tableName = "tasks", indices = [Index(value = ["deletedAt", "archivedAt"]), Index(value = ["dueDate"])])
data class TaskRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val details: String = "",
    val dueDate: String? = null,
    val ruleId: String? = null,
    val completedAt: Long? = null,
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val revision: Long = 0
)

@Entity(tableName = "notes", indices = [Index(value = ["deletedAt", "archivedAt"])])
data class NoteRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String = "",
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val revision: Long = 0
)

/** ISO local dates keep occurrences independent of clock/DST transitions. */
@Entity(
    tableName = "task_occurrences",
    indices = [Index(value = ["taskId", "dateKey"], unique = true)]
)
data class TaskOccurrence(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val dateKey: String,
    val completedAt: Long? = null
)

@Entity(tableName = "recurrence_rules")
data class RecurrenceRule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val frequency: String, // DAILY, WEEKLY, WEEKDAYS, MONTHLY, CUSTOM
    val interval: Int = 1,
    val weekdaysMask: Int = 0, // bit0 Monday ... bit6 Sunday
    val monthDay: Int? = null,
    val startDate: String, // ISO yyyy-MM-dd
    val zoneId: String,
    val createdAt: Long = System.currentTimeMillis()
)
