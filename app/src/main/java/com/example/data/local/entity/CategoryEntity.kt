package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.PlaylistCategory

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId", "name"], unique = true),
        Index(value = ["playlistId", "position"])
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val name: String,
    val position: Int = 0,
    val isVisible: Boolean = true
) {
    fun toDomain(channelCount: Int = 0): PlaylistCategory {
        return PlaylistCategory(
            id = id,
            playlistId = playlistId,
            name = name,
            position = position,
            isVisible = isVisible,
            channelCount = channelCount
        )
    }

    companion object {
        fun fromDomain(category: PlaylistCategory): CategoryEntity {
            return CategoryEntity(
                id = category.id,
                playlistId = category.playlistId,
                name = category.name,
                position = category.position,
                isVisible = category.isVisible
            )
        }
    }
}
