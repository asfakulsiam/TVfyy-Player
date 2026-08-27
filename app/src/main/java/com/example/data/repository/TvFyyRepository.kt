package com.example.data.repository

import com.example.data.local.TvFyyDatabase
import com.example.data.local.entity.FavoriteEntity
import com.example.data.local.entity.PlaybackHistoryEntity
import com.example.data.local.entity.UrlProfileEntity
import com.example.domain.model.UrlProfile
import com.example.resolver.UrlAnalyzer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

class TvFyyRepository(
    private val database: TvFyyDatabase,
    private val urlAnalyzer: UrlAnalyzer = UrlAnalyzer()
) {

    // History Flow
    val allHistory: Flow<List<PlaybackHistoryEntity>> = database.historyDao().getAllHistory()
    val recentHistory: Flow<List<PlaybackHistoryEntity>> = database.historyDao().getRecentHistory(10)

    suspend fun saveHistory(
        title: String,
        url: String,
        durationMs: Long,
        lastPositionMs: Long,
        mediaType: String,
        isLocal: Boolean = false,
        thumbnailUri: String? = null
    ) {
        val existing = database.historyDao().getHistoryByUrl(url)
        val entity = PlaybackHistoryEntity(
            id = existing?.id ?: 0L,
            title = title,
            url = url,
            durationMs = durationMs,
            lastPositionMs = lastPositionMs,
            lastPlayedAt = System.currentTimeMillis(),
            mediaType = mediaType,
            isLocal = isLocal,
            thumbnailUri = thumbnailUri
        )
        database.historyDao().insertHistory(entity)
    }

    suspend fun getHistoryItem(url: String): PlaybackHistoryEntity? {
        return database.historyDao().getHistoryByUrl(url)
    }

    suspend fun deleteHistory(id: Long) {
        database.historyDao().deleteById(id)
    }

    suspend fun clearHistory() {
        database.historyDao().clearAllHistory()
    }

    // Favorites Flow
    val allFavorites: Flow<List<FavoriteEntity>> = database.favoriteDao().getAllFavorites()

    fun isFavorite(url: String): Flow<Boolean> = database.favoriteDao().isFavoriteFlow(url)

    suspend fun toggleFavorite(
        title: String,
        url: String,
        mediaType: String,
        headers: Map<String, String> = emptyMap()
    ): Boolean {
        val exists = database.favoriteDao().isFavorite(url)
        return if (exists) {
            database.favoriteDao().deleteByUrl(url)
            false
        } else {
            val headersJson = if (headers.isNotEmpty()) JSONObject(headers).toString() else null
            val entity = FavoriteEntity(
                title = title,
                url = url,
                mediaType = mediaType,
                userAgent = headers["User-Agent"],
                referer = headers["Referer"],
                authorization = headers["Authorization"],
                customHeadersJson = headersJson
            )
            database.favoriteDao().insertFavorite(entity)
            true
        }
    }

    suspend fun deleteFavorite(id: Long) {
        database.favoriteDao().deleteById(id)
    }

    // Profiles Flow
    val allProfiles: Flow<List<UrlProfile>> = database.profileDao().getAllProfiles().map { entities ->
        entities.map { entity ->
            val customHeaders = mutableMapOf<String, String>()
            entity.customHeadersJson?.let { jsonStr ->
                try {
                    val json = JSONObject(jsonStr)
                    json.keys().forEach { key ->
                        customHeaders[key] = json.getString(key)
                    }
                } catch (_: Exception) {}
            }
            UrlProfile(
                id = entity.id,
                name = entity.name,
                userAgent = entity.userAgent,
                referer = entity.referer,
                authorization = entity.authorization,
                cookies = entity.cookies,
                customHeaders = customHeaders,
                createdAt = entity.createdAt
            )
        }
    }

    suspend fun saveProfile(profile: UrlProfile): Long {
        val jsonStr = if (profile.customHeaders.isNotEmpty()) JSONObject(profile.customHeaders).toString() else null
        val entity = UrlProfileEntity(
            id = profile.id,
            name = profile.name,
            userAgent = profile.userAgent,
            referer = profile.referer,
            authorization = profile.authorization,
            cookies = profile.cookies,
            customHeadersJson = jsonStr,
            createdAt = profile.createdAt
        )
        return database.profileDao().insertProfile(entity)
    }

    suspend fun deleteProfile(id: Long) {
        database.profileDao().deleteById(id)
    }

    // URL Analyzer wrapper
    suspend fun analyzeUrl(url: String, headers: Map<String, String> = emptyMap()): UrlAnalyzer.AnalysisResult {
        return urlAnalyzer.analyze(url, headers)
    }
}
