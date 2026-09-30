package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.UserNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserNoteDao {
    @Query("SELECT * FROM user_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<UserNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: UserNoteEntity): Long

    @Query("DELETE FROM user_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("DELETE FROM user_notes")
    suspend fun clearAll()
}
