package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.converter.MapTypeConverter
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.ChannelDao
import com.example.data.local.dao.FavoriteDao
import com.example.data.local.dao.HistoryDao
import com.example.data.local.dao.PlaylistDao
import com.example.data.local.dao.ProfileDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.FavoriteEntity
import com.example.data.local.entity.PlaybackHistoryEntity
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.UrlProfileEntity

@Database(
    entities = [
        PlaybackHistoryEntity::class,
        FavoriteEntity::class,
        UrlProfileEntity::class,
        PlaylistEntity::class,
        CategoryEntity::class,
        ChannelEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(MapTypeConverter::class)
abstract class TvFyyDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun profileDao(): ProfileDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun categoryDao(): CategoryDao
    abstract fun channelDao(): ChannelDao

    companion object {
        @Volatile
        private var INSTANCE: TvFyyDatabase? = null

        fun getDatabase(context: Context): TvFyyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TvFyyDatabase::class.java,
                    "tvfyy_player_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
