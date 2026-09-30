package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.preferences.JarvisPreferences
import com.example.data.repository.ChatRepository
import com.example.data.repository.MemoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class JarvisApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this) }
    val preferences by lazy { JarvisPreferences(this) }
    val chatRepository by lazy { ChatRepository(database.chatMessageDao()) }
    val memoryRepository by lazy {
        MemoryRepository(
            database.userMemoryDao(),
            database.userNoteDao(),
            database.userReminderDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Auto-prune expired chat messages according to user retention period
        applicationScope.launch {
            val retention = preferences.retentionPeriod.first()
            chatRepository.pruneExpiredMessages(retention)
        }
    }
}
