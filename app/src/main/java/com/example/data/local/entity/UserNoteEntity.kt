package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_notes")
data class UserNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val encryptedTitle: String,
    val encryptedContent: String,
    val timestamp: Long = System.currentTimeMillis()
)
