package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_reminders")
data class UserReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val encryptedTitle: String,
    val dueTime: Long,
    val isCompleted: Boolean = false,
    val createdTime: Long = System.currentTimeMillis()
)
