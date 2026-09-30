package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.UserMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMemoryDao {
    @Query("SELECT * FROM user_memories ORDER BY lastUpdated DESC")
    fun getAllMemories(): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memories ORDER BY lastUpdated DESC")
    suspend fun getAllMemoriesList(): List<UserMemoryEntity>

    @Query("SELECT * FROM user_memories WHERE category = :category ORDER BY lastUpdated DESC")
    suspend fun getMemoriesByCategory(category: String): List<UserMemoryEntity>

    @Query("SELECT * FROM user_memories WHERE memoryKey = :key LIMIT 1")
    suspend fun getMemoryByKey(key: String): UserMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: UserMemoryEntity): Long

    @Query("DELETE FROM user_memories WHERE memoryKey = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM user_memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM user_memories")
    suspend fun clearAll()
}
