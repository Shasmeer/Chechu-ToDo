package com.obsidianwidget.local

import android.content.Context
import androidx.room.*
import java.util.UUID

/**
 * Offline V3 records. V2 Obsidian Markdown files are intentionally never migrated or mutated
 * by database initialization. Existing widget configuration is preserved.
 */
@Entity(tableName = "tasks")
data class TaskRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val details: String = "",
    val dueEpochDay: Long? = null,
    val repeatRule: String = "NONE", // NONE, DAILY, WEEKDAYS, WEEKLY
    val completed: Boolean = false,
    val completedAtMillis: Long? = null,
    val archivedAtMillis: Long? = null,
    val deletedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val revision: Long = 0
)

@Entity(
    tableName = "task_completions",
    foreignKeys = [ForeignKey(
        entity = TaskRecord::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("taskId")]
)
data class TaskCompletionRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val completedAtMillis: Long = System.currentTimeMillis(),
    val scheduledEpochDay: Long? = null
)

@Entity(tableName = "notes")
data class NoteRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val archivedAtMillis: Long? = null,
    val deletedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val revision: Long = 0
)

@Dao
interface ChechuDao {
    @Query("SELECT * FROM tasks WHERE deletedAtMillis IS NULL AND archivedAtMillis IS NULL ORDER BY completed ASC, CASE WHEN dueEpochDay IS NULL THEN 1 ELSE 0 END, dueEpochDay ASC, createdAtMillis DESC")
    fun activeTasks(): List<TaskRecord>
    @Query("SELECT * FROM notes WHERE deletedAtMillis IS NULL AND archivedAtMillis IS NULL ORDER BY updatedAtMillis DESC")
    fun activeNotes(): List<NoteRecord>
    @Query("SELECT * FROM tasks WHERE archivedAtMillis IS NOT NULL AND deletedAtMillis IS NULL ORDER BY archivedAtMillis DESC")
    fun archivedTasks(): List<TaskRecord>
    @Query("SELECT * FROM notes WHERE archivedAtMillis IS NOT NULL AND deletedAtMillis IS NULL ORDER BY archivedAtMillis DESC")
    fun archivedNotes(): List<NoteRecord>
    @Query("SELECT * FROM tasks WHERE deletedAtMillis IS NOT NULL ORDER BY deletedAtMillis DESC")
    fun trashedTasks(): List<TaskRecord>
    @Query("SELECT * FROM notes WHERE deletedAtMillis IS NOT NULL ORDER BY deletedAtMillis DESC")
    fun trashedNotes(): List<NoteRecord>
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun task(id: String): TaskRecord?
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun note(id: String): NoteRecord?
    @Insert fun insertTask(item: TaskRecord)
    @Insert fun insertNote(item: NoteRecord)
    @Insert fun insertCompletion(item: TaskCompletionRecord)
    @Update fun updateTask(item: TaskRecord)
    @Update fun updateNote(item: NoteRecord)
    @Query("DELETE FROM tasks WHERE id = :id AND deletedAtMillis IS NOT NULL")
    fun purgeTrashedTask(id: String)
    @Query("DELETE FROM notes WHERE id = :id AND deletedAtMillis IS NOT NULL")
    fun purgeTrashedNote(id: String)
    @Query("SELECT * FROM task_completions WHERE taskId = :id ORDER BY completedAtMillis DESC")
    fun completionHistory(id: String): List<TaskCompletionRecord>
}

@Database(entities = [TaskRecord::class, NoteRecord::class, TaskCompletionRecord::class], version = 1, exportSchema = false)
abstract class ChechuDatabase : RoomDatabase() {
    abstract fun records(): ChechuDao

    companion object {
        @Volatile private var instance: ChechuDatabase? = null

        fun get(context: Context): ChechuDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, ChechuDatabase::class.java, "chechu_v3.db"
                ).build().also { instance = it }
            }
    }
}
