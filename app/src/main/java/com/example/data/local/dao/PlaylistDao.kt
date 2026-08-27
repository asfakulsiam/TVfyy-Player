package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

data class PlaylistWithCounts(
    val id: Long,
    val name: String,
    val sourceType: String,
    val sourceUrl: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastSyncedAt: Long?,
    val isActive: Boolean,
    val categoryCount: Int,
    val channelCount: Int
)

@Dao
interface PlaylistDao {

    @Query("""
        SELECT 
            p.id, p.name, p.sourceType, p.sourceUrl, p.createdAt, p.updatedAt, p.lastSyncedAt, p.isActive,
            (SELECT COUNT(*) FROM categories c WHERE c.playlistId = p.id) AS categoryCount,
            (SELECT COUNT(*) FROM channels ch WHERE ch.playlistId = p.id) AS channelCount
        FROM playlists p
        ORDER BY p.updatedAt DESC
    """)
    fun getAllPlaylistsWithCountsFlow(): Flow<List<PlaylistWithCounts>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    fun getPlaylistByIdFlow(id: Long): Flow<PlaylistEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :newName, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renamePlaylist(id: Long, newName: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE playlists SET lastSyncedAt = :syncedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateLastSynced(id: Long, syncedAt: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)

    @Query("UPDATE playlists SET isActive = 0")
    suspend fun clearActivePlaylists()

    @Query("UPDATE playlists SET isActive = 1 WHERE id = :id")
    suspend fun markPlaylistActive(id: Long)

    @Transaction
    suspend fun setActivePlaylist(id: Long) {
        clearActivePlaylists()
        markPlaylistActive(id)
    }
}
