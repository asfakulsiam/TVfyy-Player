package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val url: String,
    val durationMs: Long,
    val lastPositionMs: Long,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val mediaType: String,
    val isLocal: Boolean = false,
    val thumbnailUri: String? = null
)
