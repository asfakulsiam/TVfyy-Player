package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.UrlProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM url_profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): Flow<List<UrlProfileEntity>>

    @Query("SELECT * FROM url_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): UrlProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(entity: UrlProfileEntity): Long

    @Update
    suspend fun updateProfile(entity: UrlProfileEntity)

    @Query("DELETE FROM url_profiles WHERE id = :id")
    suspend fun deleteById(id: Long)
}
