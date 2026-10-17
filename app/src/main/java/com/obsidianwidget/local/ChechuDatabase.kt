package com.obsidianwidget.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TaskRecord::class, NoteRecord::class, TaskOccurrence::class, RecurrenceRule::class],
    version = 1,
    exportSchema = false
)
abstract class ChechuDatabase : RoomDatabase() {
    abstract fun records(): LocalRecordsDao

    companion object {
        @Volatile private var instance: ChechuDatabase? = null

        fun get(context: Context): ChechuDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChechuDatabase::class.java,
                    "chechu-local-v3.db"
                )
                    // Deliberately no destructive migration or main-thread query.
                    .build().also { instance = it }
            }
    }
}
