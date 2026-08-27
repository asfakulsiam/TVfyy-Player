package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY position ASC, id ASC")
    fun getChannelsForPlaylistFlow(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND categoryName = :categoryName ORDER BY position ASC, id ASC")
    fun getChannelsForCategoryFlow(playlistId: Long, categoryName: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND isFavorite = 1 ORDER BY position ASC, id ASC")
    fun getFavoritesForPlaylistFlow(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY position ASC, id ASC")
    suspend fun getAllChannelsForPlaylist(playlistId: Long): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: Long): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>): List<Long>

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Update
    suspend fun updateChannels(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE id = :id")
    suspend fun deleteChannelById(id: Long)

    @Query("DELETE FROM channels WHERE id IN (:ids)")
    suspend fun deleteChannelsByIds(ids: List<Long>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteAllChannelsForPlaylist(playlistId: Long)

    @Query("""
        UPDATE channels 
        SET categoryName = :newCategoryName, categoryId = :newCategoryId, updatedAt = :updatedAt, isUserEdited = 1 
        WHERE id = :channelId
    """)
    suspend fun updateChannelCategory(
        channelId: Long,
        newCategoryId: Long?,
        newCategoryName: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE channels 
        SET categoryName = :newCategoryName, categoryId = :newCategoryId, updatedAt = :updatedAt, isUserEdited = 1 
        WHERE id IN (:channelIds)
    """)
    suspend fun bulkUpdateCategory(
        channelIds: List<Long>,
        newCategoryId: Long?,
        newCategoryName: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE channels 
        SET categoryName = :toCategoryName, categoryId = :toCategoryId, updatedAt = :updatedAt 
        WHERE playlistId = :playlistId AND categoryName = :fromCategoryName
    """)
    suspend fun mergeChannelsCategory(
        playlistId: Long,
        fromCategoryName: String,
        toCategoryId: Long?,
        toCategoryName: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("""
        SELECT * FROM channels 
        WHERE playlistId = :playlistId AND (
            LOWER(name) LIKE '%' || LOWER(:query) || '%' OR 
            LOWER(tvgName) LIKE '%' || LOWER(:query) || '%' OR 
            LOWER(tvgId) LIKE '%' || LOWER(:query) || '%' OR 
            LOWER(categoryName) LIKE '%' || LOWER(:query) || '%'
        )
        ORDER BY position ASC, id ASC
    """)
    fun searchChannelsFlow(playlistId: Long, query: String): Flow<List<ChannelEntity>>

    @Query("SELECT MAX(position) FROM channels WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int?
}
