package com.obsidianwidget.local

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate

/** All calls must run off the main thread; mutations are transactional. */
class ChechuRepository(context: Context) {
    private val db = ChechuDatabase.get(context)
    private val dao = db.records()

    fun tasks(archive: Boolean = false, trash: Boolean = false): List<TaskRecord> =
        when { trash -> dao.trashedTasks(); archive -> dao.archivedTasks(); else -> dao.activeTasks() }

    fun notes(archive: Boolean = false, trash: Boolean = false): List<NoteRecord> =
        when { trash -> dao.trashedNotes(); archive -> dao.archivedNotes(); else -> dao.activeNotes() }

    fun getTask(id: String): TaskRecord? = dao.task(id)
    fun getNote(id: String): NoteRecord? = dao.note(id)
    fun history(id: String): List<TaskCompletionRecord> = dao.completionHistory(id)

    fun saveTask(id: String?, title: String, details: String, due: Long?, rule: String): Boolean {
        val trimmed = title.trim()
        if (trimmed.isEmpty() || rule !in setOf("NONE", "DAILY", "WEEKDAYS", "WEEKLY")) return false
        return db.runInTransaction<Boolean> {
            val existing = id?.let(dao::task)
            if (id != null && existing == null) return@runInTransaction false
            if (existing?.deletedAtMillis != null) return@runInTransaction false
            val now = System.currentTimeMillis()
            val saved = if (existing == null) TaskRecord(
                title = trimmed, details = details.trim(), dueEpochDay = due, repeatRule = rule
            ) else existing.copy(
                title = trimmed, details = details.trim(), dueEpochDay = due,
                repeatRule = rule, updatedAtMillis = now
            )
            if (existing == null) dao.insertTask(saved) else dao.updateTask(saved)
            true
        }
    }

    fun saveNote(id: String?, title: String, body: String): Boolean {
        if (title.isBlank() && body.isBlank()) return false
        return db.runInTransaction<Boolean> {
            val existing = id?.let(dao::note)
            if (id != null && existing == null) return@runInTransaction false
            if (existing?.deletedAtMillis != null) return@runInTransaction false
            val now = System.currentTimeMillis()
            val saved = if (existing == null) NoteRecord(
                title = title.trim().ifBlank { "Untitled note" }, body = body
            ) else existing.copy(
                title = title.trim().ifBlank { "Untitled note" },
                body = body, updatedAtMillis = now
            )
            if (existing == null) dao.insertNote(saved) else dao.updateNote(saved)
            true
        }
    }

    /**
     * Daily/weekly recurrence keeps the rule and history, moving only the next due date.
     * A repeating task cannot be checked a second time on the same local calendar day.
     */
    fun toggleTask(id: String): Boolean = db.runInTransaction<Boolean> {
        val task = dao.task(id) ?: return@runInTransaction false
        if (task.deletedAtMillis != null || task.archivedAtMillis != null) return@runInTransaction false
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        if (task.repeatRule != "NONE") {
            val history = dao.completionHistory(id)
            val alreadyToday = history.any {
                java.time.Instant.ofEpochMilli(it.completedAtMillis)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate() == today
            }
            if (alreadyToday) return@runInTransaction false
            val current = task.dueEpochDay?.let(LocalDate::ofEpochDay) ?: today
            val from = if (current.isAfter(today)) current else today
            val next = when (task.repeatRule) {
                "DAILY" -> from.plusDays(1)
                "WEEKLY" -> from.plusWeeks(1)
                "WEEKDAYS" -> {
                    var nextDate = from.plusDays(1)
                    while (nextDate.dayOfWeek == DayOfWeek.SATURDAY ||
                        nextDate.dayOfWeek == DayOfWeek.SUNDAY) nextDate = nextDate.plusDays(1)
                    nextDate
                }
                else -> return@runInTransaction false
            }
            dao.insertCompletion(TaskCompletionRecord(taskId = id, scheduledEpochDay = task.dueEpochDay))
            dao.updateTask(task.copy(
                dueEpochDay = next.toEpochDay(), completed = false,
                completedAtMillis = null, updatedAtMillis = now
            ))
        } else {
            if (!task.completed) dao.insertCompletion(TaskCompletionRecord(
                taskId = id, scheduledEpochDay = task.dueEpochDay
            ))
            dao.updateTask(task.copy(
                completed = !task.completed,
                completedAtMillis = if (task.completed) null else now,
                updatedAtMillis = now
            ))
        }
        true
    }

    fun changeState(id: String, isNote: Boolean, operation: String): Boolean =
        db.runInTransaction<Boolean> {
            val now = System.currentTimeMillis()
            if (isNote) {
                val record = dao.note(id) ?: return@runInTransaction false
                val newRecord = when (operation) {
                    "archive" -> if (record.deletedAtMillis == null) record.copy(archivedAtMillis = now) else return@runInTransaction false
                    "trash" -> record.copy(deletedAtMillis = now)
                    "restore" -> record.copy(deletedAtMillis = null, archivedAtMillis = null)
                    "unarchive" -> record.copy(archivedAtMillis = null)
                    "purge" -> { dao.purgeTrashedNote(id); return@runInTransaction record.deletedAtMillis != null }
                    else -> return@runInTransaction false
                }
                dao.updateNote(newRecord.copy(updatedAtMillis = now))
            } else {
                val record = dao.task(id) ?: return@runInTransaction false
                val newRecord = when (operation) {
                    "archive" -> if (record.deletedAtMillis == null) record.copy(archivedAtMillis = now) else return@runInTransaction false
                    "trash" -> record.copy(deletedAtMillis = now)
                    "restore" -> record.copy(deletedAtMillis = null, archivedAtMillis = null)
                    "unarchive" -> record.copy(archivedAtMillis = null)
                    "purge" -> { dao.purgeTrashedTask(id); return@runInTransaction record.deletedAtMillis != null }
                    else -> return@runInTransaction false
                }
                dao.updateTask(newRecord.copy(updatedAtMillis = now))
            }
            true
        }
}
