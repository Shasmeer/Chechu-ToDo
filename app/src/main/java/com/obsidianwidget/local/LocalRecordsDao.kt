package com.obsidianwidget.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalRecordsDao {
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND archivedAt IS NULL ORDER BY createdAt DESC")
    fun activeTasks(): List<TaskRecord>
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND archivedAt IS NOT NULL ORDER BY archivedAt DESC")
    fun archivedTasks(): List<TaskRecord>
    @Query("SELECT * FROM tasks WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun trashedTasks(): List<TaskRecord>
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun task(id: String): TaskRecord?
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insertTask(task: TaskRecord)
    @Query("UPDATE tasks SET title = :title, details = :details, updatedAt = :now, revision = revision+1 WHERE id=:id AND revision=:expectedRevision AND deletedAt IS NULL")
    fun editTask(id: String, title: String, details: String, expectedRevision: Long, now: Long): Int
    @Query("UPDATE tasks SET completedAt = :at, updatedAt = :now, revision = revision+1 WHERE id=:id AND revision=:expectedRevision AND deletedAt IS NULL AND archivedAt IS NULL")
    fun completeTask(id: String, at: Long?, expectedRevision: Long, now: Long): Int
    @Query("UPDATE tasks SET archivedAt = :at, updatedAt = :now, revision = revision+1 WHERE id=:id AND revision=:expectedRevision AND deletedAt IS NULL")
    fun archiveTask(id: String, at: Long?, expectedRevision: Long, now: Long): Int
    @Query("UPDATE tasks SET deletedAt = :at, updatedAt = :now, revision = revision+1 WHERE id=:id AND revision=:expectedRevision")
    fun trashTask(id: String, at: Long?, expectedRevision: Long, now: Long): Int
    @Query("DELETE FROM tasks WHERE id=:id AND deletedAt IS NOT NULL")
    fun purgeTask(id: String): Int
    @Query("UPDATE tasks SET ruleId = :ruleId, completedAt = NULL, updatedAt = :now, revision = revision+1 WHERE id=:id AND revision=:expectedRevision AND deletedAt IS NULL")
    fun setTaskRule(id: String, ruleId: String?, expectedRevision: Long, now: Long): Int

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND archivedAt IS NULL ORDER BY updatedAt DESC")
    fun activeNotes(): List<NoteRecord>
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND archivedAt IS NOT NULL ORDER BY archivedAt DESC")
    fun archivedNotes(): List<NoteRecord>
    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun trashedNotes(): List<NoteRecord>
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun note(id: String): NoteRecord?
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insertNote(note: NoteRecord)
    @Query("UPDATE notes SET title=:title, body=:body, updatedAt=:now, revision=revision+1 WHERE id=:id AND revision=:expectedRevision AND deletedAt IS NULL")
    fun editNote(id: String, title: String, body: String, expectedRevision: Long, now: Long): Int
    @Query("UPDATE notes SET archivedAt=:at, updatedAt=:now, revision=revision+1 WHERE id=:id AND revision=:expectedRevision AND deletedAt IS NULL")
    fun archiveNote(id: String, at: Long?, expectedRevision: Long, now: Long): Int
    @Query("UPDATE notes SET deletedAt=:at, updatedAt=:now, revision=revision+1 WHERE id=:id AND revision=:expectedRevision")
    fun trashNote(id: String, at: Long?, expectedRevision: Long, now: Long): Int
    @Query("DELETE FROM notes WHERE id=:id AND deletedAt IS NOT NULL")
    fun purgeNote(id: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT) fun insertRule(rule: RecurrenceRule)
    @Query("SELECT * FROM recurrence_rules WHERE id=:id LIMIT 1")
    fun rule(id: String): RecurrenceRule?
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insertOccurrence(occurrence: TaskOccurrence)
    @Query("SELECT * FROM task_occurrences WHERE taskId=:taskId AND dateKey=:date LIMIT 1")
    fun occurrence(taskId: String, date: String): TaskOccurrence?
    @Query("UPDATE task_occurrences SET completedAt = :at WHERE taskId=:taskId AND dateKey=:date")
    fun setOccurrenceComplete(taskId: String, date: String, at: Long?): Int
    @Query("DELETE FROM task_occurrences WHERE taskId=:taskId")
    fun clearOccurrences(taskId: String)
}
