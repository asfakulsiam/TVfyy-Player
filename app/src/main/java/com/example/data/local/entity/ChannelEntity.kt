package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.PlaylistChannel

@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId", "position"]),
        Index(value = ["playlistId", "categoryId"]),
        Index(value = ["playlistId", "categoryName"]),
        Index(value = ["playlistId", "name"]),
        Index(value = ["playlistId", "streamUrl"]),
        Index(value = ["playlistId", "tvgId"])
    ]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val categoryId: Long? = null,
    val categoryName: String = "Uncategorized",
    val name: String,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val tvgLogo: String? = null,
    val streamUrl: String,
    val position: Int = 0,
    val isFavorite: Boolean = false,
    val isUserEdited: Boolean = false,
    val knownAttributes: Map<String, String> = emptyMap(),
    val unknownAttributes: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): PlaylistChannel {
        return PlaylistChannel(
            id = id,
            playlistId = playlistId,
            categoryId = categoryId,
            categoryName = categoryName,
            name = name,
            tvgId = tvgId,
            tvgName = tvgName,
            tvgLogo = tvgLogo,
            streamUrl = streamUrl,
            position = position,
            isFavorite = isFavorite,
            isUserEdited = isUserEdited,
            knownAttributes = knownAttributes,
            unknownAttributes = unknownAttributes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(channel: PlaylistChannel): ChannelEntity {
            return ChannelEntity(
                id = channel.id,
                playlistId = channel.playlistId,
                categoryId = channel.categoryId,
                categoryName = channel.categoryName,
                name = channel.name,
                tvgId = channel.tvgId,
                tvgName = channel.tvgName,
                tvgLogo = channel.tvgLogo,
                streamUrl = channel.streamUrl,
                position = channel.position,
                isFavorite = channel.isFavorite,
                isUserEdited = channel.isUserEdited,
                knownAttributes = channel.knownAttributes,
                unknownAttributes = channel.unknownAttributes,
                createdAt = channel.createdAt,
                updatedAt = channel.updatedAt
            )
        }
    }
}
