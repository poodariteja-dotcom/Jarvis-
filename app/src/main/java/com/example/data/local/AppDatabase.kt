package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.UserMemoryDao
import com.example.data.local.dao.UserNoteDao
import com.example.data.local.dao.UserReminderDao
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.UserMemoryEntity
import com.example.data.local.entity.UserNoteEntity
import com.example.data.local.entity.UserReminderEntity

@Database(
    entities = [
        ChatMessageEntity::class,
        UserMemoryEntity::class,
        UserNoteEntity::class,
        UserReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun userMemoryDao(): UserMemoryDao
    abstract fun userNoteDao(): UserNoteDao
    abstract fun userReminderDao(): UserReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jarvis_encrypted_vault.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
