package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC")
    fun getAllHistory(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 10): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE url = :url LIMIT 1")
    suspend fun getHistoryByUrl(url: String): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: PlaybackHistoryEntity): Long

    @Update
    suspend fun updateHistory(entity: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM playback_history")
    suspend fun clearAllHistory()
}
