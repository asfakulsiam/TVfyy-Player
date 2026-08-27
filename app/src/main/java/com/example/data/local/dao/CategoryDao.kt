package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

data class CategoryWithCount(
    val id: Long,
    val playlistId: Long,
    val name: String,
    val position: Int,
    val isVisible: Boolean,
    val channelCount: Int
)

@Dao
interface CategoryDao {

    @Query("""
        SELECT 
            c.id, c.playlistId, c.name, c.position, c.isVisible,
            (SELECT COUNT(*) FROM channels ch WHERE ch.playlistId = c.playlistId AND ch.categoryName = c.name) AS channelCount
        FROM categories c
        WHERE c.playlistId = :playlistId
        ORDER BY c.position ASC, c.name ASC
    """)
    fun getCategoriesWithCountsFlow(playlistId: Long): Flow<List<CategoryWithCount>>

    @Query("SELECT * FROM categories WHERE playlistId = :playlistId ORDER BY position ASC, name ASC")
    suspend fun getCategoriesForPlaylist(playlistId: Long): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE playlistId = :playlistId AND name = :name LIMIT 1")
    suspend fun getCategoryByName(playlistId: Long, name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategories(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET name = :newName WHERE id = :id")
    suspend fun renameCategory(id: Long, newName: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("DELETE FROM categories WHERE playlistId = :playlistId")
    suspend fun deleteAllCategoriesForPlaylist(playlistId: Long)

    @Query("UPDATE categories SET isVisible = :isVisible WHERE id = :id")
    suspend fun setCategoryVisibility(id: Long, isVisible: Boolean)
}
