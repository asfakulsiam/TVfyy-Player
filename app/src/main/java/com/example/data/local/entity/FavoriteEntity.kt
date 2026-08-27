package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val url: String,
    val mediaType: String,
    val userAgent: String? = null,
    val referer: String? = null,
    val authorization: String? = null,
    val customHeadersJson: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
