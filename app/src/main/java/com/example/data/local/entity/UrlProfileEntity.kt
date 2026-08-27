package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "url_profiles")
data class UrlProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val userAgent: String? = null,
    val referer: String? = null,
    val authorization: String? = null,
    val cookies: String? = null,
    val customHeadersJson: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
