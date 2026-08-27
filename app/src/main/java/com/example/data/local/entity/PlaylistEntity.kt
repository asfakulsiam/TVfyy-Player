package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Playlist
import com.example.domain.model.PlaylistSourceType

@Entity(
    tableName = "playlists",
    indices = [
        Index(value = ["name"]),
        Index(value = ["updatedAt"])
    ]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sourceType: String = PlaylistSourceType.MANUAL.name,
    val sourceUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long? = null,
    val isActive: Boolean = false
) {
    fun toDomain(categoryCount: Int = 0, channelCount: Int = 0): Playlist {
        return Playlist(
            id = id,
            name = name,
            sourceType = try { PlaylistSourceType.valueOf(sourceType) } catch (_: Exception) { PlaylistSourceType.MANUAL },
            sourceUrl = sourceUrl,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastSyncedAt = lastSyncedAt,
            categoryCount = categoryCount,
            channelCount = channelCount,
            isActive = isActive
        )
    }

    companion object {
        fun fromDomain(playlist: Playlist): PlaylistEntity {
            return PlaylistEntity(
                id = playlist.id,
                name = playlist.name,
                sourceType = playlist.sourceType.name,
                sourceUrl = playlist.sourceUrl,
                createdAt = playlist.createdAt,
                updatedAt = playlist.updatedAt,
                lastSyncedAt = playlist.lastSyncedAt,
                isActive = playlist.isActive
            )
        }
    }
}
