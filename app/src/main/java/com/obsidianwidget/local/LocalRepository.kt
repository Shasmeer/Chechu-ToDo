package com.obsidianwidget.local

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Room is canonical only for newly created local records.
 * No automatic import, mutation or migration of the existing Obsidian vault.
 */
object LocalRepository {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun <T> async(context: Context, operation: (ChechuDatabase) -> T, callback: (Result<T>) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            val answer = runCatching { operation(ChechuDatabase.get(app)) }
            main.post { callback(answer) }
        }
    }

    fun addTask(db: ChechuDatabase, title: String, details: String): TaskRecord {
        require(title.isNotBlank()) { "Task title required" }
        return TaskRecord(title = title.trim(), details = details).also { db.records().insertTask(it) }
    }

    fun addNote(db: ChechuDatabase, title: String, body: String): NoteRecord {
        require(title.isNotBlank() || body.isNotBlank()) { "Note cannot be empty" }
        val normalizedTitle = title.trim().ifEmpty { body.lineSequence().firstOrNull()?.take(56) ?: "Untitled" }
        return NoteRecord(title = normalizedTitle, body = body).also { db.records().insertNote(it) }
    }

    fun toggle(db: ChechuDatabase, id: String): Boolean = db.runInTransaction<Boolean> {
        val dao = db.records()
        val task = dao.task(id) ?: return@runInTransaction false
        if (task.archivedAt != null || task.deletedAt != null) return@runInTransaction false
        val now = System.currentTimeMillis()
        val rule = task.ruleId?.let(dao::rule)
        if (rule == null) {
            dao.completeTask(id, if (task.completedAt == null) now else null, task.revision, now) == 1
        } else {
            val date = LocalDate.now(ZoneId.of(rule.zoneId))
            if (!RecurrenceEngine.isScheduled(rule, date)) return@runInTransaction false
            if (dao.touchTask(id, task.revision, now) != 1) return@runInTransaction false
            val key = date.toString()
            val current = dao.occurrence(id, key)
            if (current == null) {
                dao.insertOccurrence(TaskOccurrence(taskId = id, dateKey = key, completedAt = now))
                true
            } else dao.setOccurrenceComplete(id, key, if (current.completedAt == null) now else null) == 1
        }
    }

    fun setRecurrence(db: ChechuDatabase, taskId: String, frequency: String, interval: Int = 1, daysMask: Int = 0): Boolean =
        db.runInTransaction<Boolean> {
            val dao = db.records()
            val task = dao.task(taskId) ?: return@runInTransaction false
            if (task.deletedAt != null || interval < 1) return@runInTransaction false
            val now = System.currentTimeMillis()
            if (frequency == "NONE") {
                dao.setTaskRule(taskId, null, task.revision, now) == 1
            } else {
                val zone = ZoneId.systemDefault()
                val date = LocalDate.now(zone)
                val rule = RecurrenceRule(
                    id = UUID.randomUUID().toString(),
                    frequency = frequency,
                    interval = interval,
                    weekdaysMask = daysMask,
                    monthDay = date.dayOfMonth,
                    startDate = date.toString(),
                    zoneId = zone.id
                )
                dao.insertRule(rule)
                dao.setTaskRule(taskId, rule.id, task.revision, now) == 1
            }
        }

    fun isCompleteToday(db: ChechuDatabase, task: TaskRecord): Boolean {
        val rule = task.ruleId?.let(db.records()::rule) ?: return task.completedAt != null
        val date = LocalDate.now(ZoneId.of(rule.zoneId))
        return db.records().occurrence(task.id, date.toString())?.completedAt != null
    }

    fun nextDueLabel(db: ChechuDatabase, task: TaskRecord): String? {
        val rule = task.ruleId?.let(db.records()::rule) ?: return null
        val date = LocalDate.now(ZoneId.of(rule.zoneId))
        val next = RecurrenceEngine.nextOnOrAfter(rule, date) ?: return null
        return if (next == date) "Repeats ${rule.frequency.lowercase()}" else "Next: $next"
    }
}
