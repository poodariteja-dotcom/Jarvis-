package com.example.data.repository

import com.example.data.crypto.CryptoManager
import com.example.data.local.dao.UserMemoryDao
import com.example.data.local.dao.UserNoteDao
import com.example.data.local.dao.UserReminderDao
import com.example.data.local.entity.UserMemoryEntity
import com.example.data.local.entity.UserNoteEntity
import com.example.data.local.entity.UserReminderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class UserMemory(
    val id: Long,
    val category: String,
    val key: String,
    val value: String,
    val lastUpdated: Long
)

data class UserNote(
    val id: Long,
    val title: String,
    val content: String,
    val timestamp: Long
)

data class UserReminder(
    val id: Long,
    val title: String,
    val dueTime: Long,
    val isCompleted: Boolean,
    val createdTime: Long
)

class MemoryRepository(
    private val memoryDao: UserMemoryDao,
    private val noteDao: UserNoteDao,
    private val reminderDao: UserReminderDao
) {
    val allMemories: Flow<List<UserMemory>> = memoryDao.getAllMemories().map { list ->
        list.map {
            UserMemory(
                id = it.id,
                category = it.category,
                key = it.memoryKey,
                value = CryptoManager.decrypt(it.encryptedValue),
                lastUpdated = it.lastUpdated
            )
        }
    }

    val allNotes: Flow<List<UserNote>> = noteDao.getAllNotes().map { list ->
        list.map {
            UserNote(
                id = it.id,
                title = CryptoManager.decrypt(it.encryptedTitle),
                content = CryptoManager.decrypt(it.encryptedContent),
                timestamp = it.timestamp
            )
        }
    }

    val allReminders: Flow<List<UserReminder>> = reminderDao.getAllReminders().map { list ->
        list.map {
            UserReminder(
                id = it.id,
                title = CryptoManager.decrypt(it.encryptedTitle),
                dueTime = it.dueTime,
                isCompleted = it.isCompleted,
                createdTime = it.createdTime
            )
        }
    }

    suspend fun saveMemory(category: String, key: String, value: String) {
        val encrypted = CryptoManager.encrypt(value)
        val entity = UserMemoryEntity(
            category = category,
            memoryKey = key,
            encryptedValue = encrypted,
            lastUpdated = System.currentTimeMillis()
        )
        memoryDao.insertMemory(entity)
    }

    suspend fun getMemorySummaryForPrompt(): String {
        val list = memoryDao.getAllMemoriesList()
        if (list.isEmpty()) return ""
        val builder = StringBuilder("KNOWN USER PREFERENCES & MEMORIES:\n")
        list.take(15).forEach {
            val decrypted = CryptoManager.decrypt(it.encryptedValue)
            builder.append("- [${it.category}] ${it.memoryKey}: $decrypted\n")
        }
        return builder.toString()
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteById(id)
    }

    suspend fun saveNote(title: String, content: String): Long {
        return noteDao.insertNote(
            UserNoteEntity(
                encryptedTitle = CryptoManager.encrypt(title),
                encryptedContent = CryptoManager.encrypt(content),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteNote(id: Long) {
        noteDao.deleteNote(id)
    }

    suspend fun saveReminder(title: String, dueTime: Long): Long {
        return reminderDao.insertReminder(
            UserReminderEntity(
                encryptedTitle = CryptoManager.encrypt(title),
                dueTime = dueTime,
                isCompleted = false,
                createdTime = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleReminder(id: Long, completed: Boolean) {
        reminderDao.setCompleted(id, completed)
    }

    suspend fun deleteReminder(id: Long) {
        reminderDao.deleteReminder(id)
    }

    suspend fun clearAllPersonalData() {
        memoryDao.clearAll()
        noteDao.clearAll()
        reminderDao.clearAll()
    }
}
