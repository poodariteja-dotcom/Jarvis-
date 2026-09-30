package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.UserReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserReminderDao {
    @Query("SELECT * FROM user_reminders ORDER BY dueTime ASC")
    fun getAllReminders(): Flow<List<UserReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: UserReminderEntity): Long

    @Query("UPDATE user_reminders SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM user_reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)

    @Query("DELETE FROM user_reminders")
    suspend fun clearAll()
}
