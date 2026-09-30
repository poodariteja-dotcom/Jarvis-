package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_memories")
data class UserMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // "PREFERENCE", "CONTACT", "ROUTINE", "NOTE", "FACT"
    val memoryKey: String, // e.g. "favorite_music_app", "frequent_contact"
    val encryptedValue: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
