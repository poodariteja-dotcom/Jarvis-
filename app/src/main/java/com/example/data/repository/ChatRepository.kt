package com.example.data.repository

import com.example.data.crypto.CryptoManager
import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.preferences.RetentionPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class MessageRole {
    USER, ASSISTANT, SYSTEM, ACTION
}

enum class ActionStatus {
    PENDING_CONFIRMATION, CONFIRMED, EXECUTED, CANCELLED
}

data class ChatMessage(
    val id: Long = 0,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null,
    val actionPayload: String? = null,
    val actionStatus: ActionStatus? = null
)

class ChatRepository(
    private val chatMessageDao: ChatMessageDao
) {
    val allMessages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun addMessage(
        role: MessageRole,
        content: String,
        actionType: String? = null,
        actionPayload: String? = null,
        actionStatus: ActionStatus? = null
    ): Long {
        val encrypted = CryptoManager.encrypt(content)
        val entity = ChatMessageEntity(
            role = role.name,
            encryptedContent = encrypted,
            timestamp = System.currentTimeMillis(),
            actionType = actionType,
            actionPayload = actionPayload,
            actionStatus = actionStatus?.name
        )
        return chatMessageDao.insertMessage(entity)
    }

    suspend fun updateActionStatus(id: Long, newStatus: ActionStatus) {
        val recent = chatMessageDao.getRecentMessages(50)
        val found = recent.firstOrNull { it.id == id } ?: return
        val updated = found.copy(actionStatus = newStatus.name)
        chatMessageDao.updateMessage(updated)
    }

    suspend fun getRecentContext(limit: Int = 10): List<ChatMessage> {
        val raw = chatMessageDao.getRecentMessages(limit)
        return raw.reversed().map { it.toDomain() }
    }

    suspend fun pruneExpiredMessages(retentionPeriod: RetentionPeriod): Int {
        if (retentionPeriod == RetentionPeriod.FOREVER) return 0
        val cutoff = System.currentTimeMillis() - retentionPeriod.millis
        return chatMessageDao.deleteMessagesOlderThan(cutoff)
    }

    suspend fun clearHistory() {
        chatMessageDao.clearAll()
    }

    private fun ChatMessageEntity.toDomain(): ChatMessage {
        val decrypted = CryptoManager.decrypt(encryptedContent)
        val domainRole = try {
            MessageRole.valueOf(role)
        } catch (e: Exception) {
            MessageRole.ASSISTANT
        }
        val domainStatus = actionStatus?.let {
            try { ActionStatus.valueOf(it) } catch (e: Exception) { null }
        }
        return ChatMessage(
            id = id,
            role = domainRole,
            content = decrypted,
            timestamp = timestamp,
            actionType = actionType,
            actionPayload = actionPayload,
            actionStatus = domainStatus
        )
    }
}
