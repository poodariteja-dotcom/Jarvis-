package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user", "assistant", "system", "action"
    val encryptedContent: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null, // "CALL", "SMS", "EMAIL", "ALARM", "OPEN_APP", "NOTE"
    val actionPayload: String? = null, // JSON details of action
    val actionStatus: String? = null // "PENDING_CONFIRMATION", "CONFIRMED", "EXECUTED", "CANCELLED"
)
