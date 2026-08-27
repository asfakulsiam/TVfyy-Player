package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.TvFyyDatabase
import com.example.data.local.dao.CategoryWithCount
import com.example.data.local.dao.PlaylistWithCounts
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.PlaylistEntity
import com.example.data.parser.M3uExportOptions
import com.example.data.parser.M3uExporter
import com.example.data.parser.M3uParser
import com.example.domain.model.ChannelDiff
import com.example.domain.model.DiffType
import com.example.domain.model.ImportMode
import com.example.domain.model.ParsedEntry
import com.example.domain.model.ParsedPlaylist
import com.example.domain.model.Playlist
import com.example.domain.model.PlaylistCategory
import com.example.domain.model.PlaylistChangeSummary
import com.example.domain.model.PlaylistChannel
import com.example.domain.model.PlaylistSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class PlaylistRepository(private val database: TvFyyDatabase) {

    private val playlistDao = database.playlistDao()
    private val categoryDao = database.categoryDao()
    private val channelDao = database.channelDao()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylistsWithCountsFlow().map { list ->
        list.map { it.toDomain() }
    }

    fun getPlaylistFlow(playlistId: Long): Flow<PlaylistEntity?> {
        return playlistDao.getPlaylistByIdFlow(playlistId)
    }

    fun getCategoriesFlow(playlistId: Long): Flow<List<PlaylistCategory>> {
        return categoryDao.getCategoriesWithCountsFlow(playlistId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getChannelsFlow(playlistId: Long): Flow<List<PlaylistChannel>> {
        return channelDao.getChannelsForPlaylistFlow(playlistId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getChannelsForCategoryFlow(playlistId: Long, categoryName: String): Flow<List<PlaylistChannel>> {
        return channelDao.getChannelsForCategoryFlow(playlistId, categoryName).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getFavoriteChannelsFlow(playlistId: Long): Flow<List<PlaylistChannel>> {
        return channelDao.getFavoritesForPlaylistFlow(playlistId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun searchChannelsFlow(playlistId: Long, query: String): Flow<List<PlaylistChannel>> {
        return channelDao.searchChannelsFlow(playlistId, query).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getPlaylist(id: Long): Playlist? = withContext(Dispatchers.IO) {
        val entity = playlistDao.getPlaylistById(id) ?: return@withContext null
        val catCount = categoryDao.getCategoriesForPlaylist(id).size
        val chanCount = channelDao.getAllChannelsForPlaylist(id).size
        entity.toDomain(categoryCount = catCount, channelCount = chanCount)
    }

    suspend fun parseFromText(content: String, defaultName: String? = null): ParsedPlaylist = withContext(Dispatchers.IO) {
        M3uParser.parse(content, defaultName)
    }

    suspend fun parseFromUri(context: Context, uri: Uri): Result<ParsedPlaylist> = withContext(Dispatchers.IO) {
        try {
            var fileName = "Playlist"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }
            } catch (_: Exception) {}

            val defaultName = fileName.substringBeforeLast(".")
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open selected file stream."))

            val parsed = inputStream.use { M3uParser.parse(it, defaultName) }
            if (parsed.entries.isEmpty() && parsed.warnings.isNotEmpty()) {
                Result.failure(Exception("No valid M3U entries found. ${parsed.warnings.firstOrNull()?.reason}"))
            } else {
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadAndParseUrl(url: String, customName: String? = null): Result<ParsedPlaylist> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = if (url.contains("github.com") && url.contains("/blob/")) {
                url.replace("github.com", "raw.githubusercontent.com").replace("/blob/", "/")
            } else {
                url
            }

            val request = Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", "TVfyyPlayer/1.1 (Linux; Android)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP Error ${response.code}: ${response.message}"))
            }

            val contentType = response.header("Content-Type", "")?.lowercase() ?: ""
            if (contentType.contains("text/html") && !url.endsWith(".m3u") && !url.endsWith(".m3u8")) {
                return@withContext Result.failure(Exception("This URL returns HTML instead of a valid M3U playlist."))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body from server."))
            val defaultName = customName ?: extractNameFromUrl(normalizedUrl)
            val parsed = M3uParser.parse(body, defaultName)

            if (parsed.entries.isEmpty()) {
                Result.failure(Exception("Downloaded file did not contain any valid media channels."))
            } else {
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importParsedPlaylist(
        parsed: ParsedPlaylist,
        playlistName: String,
        sourceType: PlaylistSourceType,
        sourceUrl: String? = null,
        mode: ImportMode = ImportMode.ADD_AS_NEW,
        targetPlaylistId: Long? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val playlistId: Long
            val now = System.currentTimeMillis()

            if (mode == ImportMode.REPLACE_EXISTING && targetPlaylistId != null) {
                playlistId = targetPlaylistId
                channelDao.deleteAllChannelsForPlaylist(playlistId)
                categoryDao.deleteAllCategoriesForPlaylist(playlistId)
                playlistDao.renamePlaylist(playlistId, playlistName, now)
                playlistDao.updateLastSynced(playlistId, now, now)
            } else if (mode == ImportMode.MERGE_EXISTING && targetPlaylistId != null) {
                playlistId = targetPlaylistId
                playlistDao.updateLastSynced(playlistId, now, now)
                return@withContext mergeParsedEntriesIntoPlaylist(playlistId, parsed.entries)
            } else {
                val newPlaylist = PlaylistEntity(
                    name = playlistName.ifBlank { "New Playlist" },
                    sourceType = sourceType.name,
                    sourceUrl = sourceUrl,
                    createdAt = now,
                    updatedAt = now,
                    lastSyncedAt = if (sourceType == PlaylistSourceType.REMOTE_URL || sourceType == PlaylistSourceType.GITHUB_RAW) now else null,
                    isActive = false
                )
                playlistId = playlistDao.insertPlaylist(newPlaylist)
            }

            // Insert Categories
            val uniqueCategories = parsed.categories.ifEmpty { listOf("Uncategorized") }
            val categoryEntities = uniqueCategories.mapIndexed { index, catName ->
                CategoryEntity(
                    playlistId = playlistId,
                    name = catName,
                    position = index,
                    isVisible = true
                )
            }
            categoryDao.insertCategories(categoryEntities)

            val categoryMap = categoryDao.getCategoriesForPlaylist(playlistId).associate { it.name to it.id }

            // Insert Channels in chunks of 500
            val channelEntities = parsed.entries.mapIndexed { index, entry ->
                val catName = entry.groupTitle?.ifBlank { "Uncategorized" } ?: "Uncategorized"
                ChannelEntity(
                    playlistId = playlistId,
                    categoryId = categoryMap[catName],
                    categoryName = catName,
                    name = entry.name,
                    tvgId = entry.tvgId,
                    tvgName = entry.tvgName,
                    tvgLogo = entry.tvgLogo,
                    streamUrl = entry.streamUrl,
                    position = index,
                    isFavorite = false,
                    isUserEdited = false,
                    knownAttributes = entry.knownAttributes,
                    unknownAttributes = entry.unknownAttributes,
                    createdAt = now,
                    updatedAt = now
                )
            }

            channelEntities.chunked(500).forEach { chunk ->
                channelDao.insertChannels(chunk)
            }

            Result.success(playlistId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun computeRefreshChanges(playlistId: Long): Result<PlaylistChangeSummary> = withContext(Dispatchers.IO) {
        try {
            val playlist = playlistDao.getPlaylistById(playlistId)
                ?: return@withContext Result.failure(Exception("Playlist not found"))
            val sourceUrl = playlist.sourceUrl
                ?: return@withContext Result.failure(Exception("Playlist has no remote source URL"))

            val remoteResult = downloadAndParseUrl(sourceUrl, playlist.name)
            if (remoteResult.isFailure) {
                return@withContext Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to fetch remote playlist"))
            }

            val remoteParsed = remoteResult.getOrThrow()
            val existingChannels = channelDao.getAllChannelsForPlaylist(playlistId)

            val existingMap = mutableMapOf<String, ChannelEntity>()
            existingChannels.forEach { ch ->
                val key = buildChannelMatchingKey(ch.tvgId, ch.streamUrl, ch.name, ch.categoryName)
                existingMap[key] = ch
            }

            val remoteKeys = mutableSetOf<String>()
            val diffList = mutableListOf<ChannelDiff>()

            var added = 0
            var removed = 0
            var modified = 0
            var unchanged = 0

            for (entry in remoteParsed.entries) {
                val groupTitle = entry.groupTitle?.ifBlank { "Uncategorized" } ?: "Uncategorized"
                val key = buildChannelMatchingKey(entry.tvgId, entry.streamUrl, entry.name, groupTitle)
                remoteKeys.add(key)

                val existing = existingMap[key]
                if (existing == null) {
                    added++
                    diffList.add(
                        ChannelDiff(
                            channel = PlaylistChannel(
                                playlistId = playlistId,
                                name = entry.name,
                                streamUrl = entry.streamUrl,
                                tvgId = entry.tvgId,
                                tvgLogo = entry.tvgLogo,
                                categoryName = groupTitle
                            ),
                            changeType = DiffType.ADDED,
                            details = "New channel in category '$groupTitle'"
                        )
                    )
                } else {
                    val hasChanged = existing.streamUrl != entry.streamUrl ||
                            existing.name != entry.name ||
                            existing.tvgLogo != entry.tvgLogo ||
                            existing.categoryName != groupTitle

                    if (hasChanged) {
                        modified++
                        diffList.add(
                            ChannelDiff(
                                channel = existing.toDomain(),
                                changeType = DiffType.MODIFIED,
                                details = if (existing.isUserEdited) "Remote updated (User local edits will be preserved)" else "Metadata or stream URL updated"
                            )
                        )
                    } else {
                        unchanged++
                    }
                }
            }

            for (existing in existingChannels) {
                val key = buildChannelMatchingKey(existing.tvgId, existing.streamUrl, existing.name, existing.categoryName)
                if (!remoteKeys.contains(key)) {
                    removed++
                    diffList.add(
                        ChannelDiff(
                            channel = existing.toDomain(),
                            changeType = DiffType.REMOVED,
                            details = "No longer present in remote playlist"
                        )
                    )
                }
            }

            val summary = PlaylistChangeSummary(
                playlistId = playlistId,
                playlistName = playlist.name,
                existingCount = existingChannels.size,
                remoteCount = remoteParsed.entries.size,
                addedCount = added,
                removedCount = removed,
                modifiedCount = modified,
                unchangedCount = unchanged,
                diffList = diffList
            )

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyRefreshChanges(
        playlistId: Long,
        keepLocalEdits: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val playlist = playlistDao.getPlaylistById(playlistId)
                ?: return@withContext Result.failure(Exception("Playlist not found"))
            val sourceUrl = playlist.sourceUrl
                ?: return@withContext Result.failure(Exception("Playlist has no remote source URL"))

            val remoteResult = downloadAndParseUrl(sourceUrl, playlist.name)
            if (remoteResult.isFailure) {
                return@withContext Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to fetch remote playlist"))
            }

            val remoteParsed = remoteResult.getOrThrow()
            val existingChannels = channelDao.getAllChannelsForPlaylist(playlistId)
            val now = System.currentTimeMillis()

            val existingMap = mutableMapOf<String, ChannelEntity>()
            existingChannels.forEach { ch ->
                val key = buildChannelMatchingKey(ch.tvgId, ch.streamUrl, ch.name, ch.categoryName)
                existingMap[key] = ch
            }

            // Sync Categories
            val existingCategories = categoryDao.getCategoriesForPlaylist(playlistId).associateBy { it.name }
            val newCategoryEntities = mutableListOf<CategoryEntity>()
            var nextCatPos = existingCategories.size

            remoteParsed.categories.forEach { catName ->
                if (!existingCategories.containsKey(catName)) {
                    newCategoryEntities.add(
                        CategoryEntity(
                            playlistId = playlistId,
                            name = catName,
                            position = nextCatPos++,
                            isVisible = true
                        )
                    )
                }
            }
            if (newCategoryEntities.isNotEmpty()) {
                categoryDao.insertCategories(newCategoryEntities)
            }

            val allCategoryMap = categoryDao.getCategoriesForPlaylist(playlistId).associate { it.name to it.id }

            val channelsToInsert = mutableListOf<ChannelEntity>()
            val channelsToUpdate = mutableListOf<ChannelEntity>()
            var maxPosition = (channelDao.getMaxPosition(playlistId) ?: -1) + 1

            for (entry in remoteParsed.entries) {
                val groupTitle = entry.groupTitle?.ifBlank { "Uncategorized" } ?: "Uncategorized"
                val key = buildChannelMatchingKey(entry.tvgId, entry.streamUrl, entry.name, groupTitle)
                val existing = existingMap[key]

                if (existing == null) {
                    // New Channel
                    channelsToInsert.add(
                        ChannelEntity(
                            playlistId = playlistId,
                            categoryId = allCategoryMap[groupTitle],
                            categoryName = groupTitle,
                            name = entry.name,
                            tvgId = entry.tvgId,
                            tvgName = entry.tvgName,
                            tvgLogo = entry.tvgLogo,
                            streamUrl = entry.streamUrl,
                            position = maxPosition++,
                            isFavorite = false,
                            isUserEdited = false,
                            knownAttributes = entry.knownAttributes,
                            unknownAttributes = entry.unknownAttributes,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                } else {
                    // Existing Channel: if keepLocalEdits and channel was edited by user, protect edited fields
                    val updatedChannel = if (keepLocalEdits && existing.isUserEdited) {
                        existing.copy(
                            streamUrl = entry.streamUrl, // update underlying stream if changed
                            updatedAt = now
                        )
                    } else {
                        existing.copy(
                            categoryId = allCategoryMap[groupTitle] ?: existing.categoryId,
                            categoryName = groupTitle,
                            name = entry.name,
                            tvgId = entry.tvgId ?: existing.tvgId,
                            tvgName = entry.tvgName ?: existing.tvgName,
                            tvgLogo = entry.tvgLogo ?: existing.tvgLogo,
                            streamUrl = entry.streamUrl,
                            knownAttributes = entry.knownAttributes,
                            unknownAttributes = entry.unknownAttributes,
                            updatedAt = now
                        )
                    }
                    channelsToUpdate.add(updatedChannel)
                }
            }

            if (channelsToInsert.isNotEmpty()) {
                channelsToInsert.chunked(500).forEach { channelDao.insertChannels(it) }
            }
            if (channelsToUpdate.isNotEmpty()) {
                channelsToUpdate.chunked(500).forEach { channelDao.updateChannels(it) }
            }

            playlistDao.updateLastSynced(playlistId, now, now)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun mergeParsedEntriesIntoPlaylist(
        playlistId: Long,
        entries: List<ParsedEntry>
    ): Result<Long> {
        val now = System.currentTimeMillis()
        val existingChannels = channelDao.getAllChannelsForPlaylist(playlistId)
        val existingMap = existingChannels.associateBy {
            buildChannelMatchingKey(it.tvgId, it.streamUrl, it.name, it.categoryName)
        }.toMutableMap()

        // Categories
        val existingCategories = categoryDao.getCategoriesForPlaylist(playlistId).associateBy { it.name }.toMutableMap()
        var nextCatPos = existingCategories.size

        val channelsToInsert = mutableListOf<ChannelEntity>()
        val channelsToUpdate = mutableListOf<ChannelEntity>()
        var maxPosition = (channelDao.getMaxPosition(playlistId) ?: -1) + 1

        for (entry in entries) {
            val groupTitle = entry.groupTitle?.ifBlank { "Uncategorized" } ?: "Uncategorized"
            if (!existingCategories.containsKey(groupTitle)) {
                val newCat = CategoryEntity(playlistId = playlistId, name = groupTitle, position = nextCatPos++, isVisible = true)
                val newCatId = categoryDao.insertCategory(newCat)
                existingCategories[groupTitle] = newCat.copy(id = newCatId)
            }

            val key = buildChannelMatchingKey(entry.tvgId, entry.streamUrl, entry.name, groupTitle)
            val existing = existingMap[key]

            if (existing == null) {
                channelsToInsert.add(
                    ChannelEntity(
                        playlistId = playlistId,
                        categoryId = existingCategories[groupTitle]?.id,
                        categoryName = groupTitle,
                        name = entry.name,
                        tvgId = entry.tvgId,
                        tvgName = entry.tvgName,
                        tvgLogo = entry.tvgLogo,
                        streamUrl = entry.streamUrl,
                        position = maxPosition++,
                        isFavorite = false,
                        isUserEdited = false,
                        knownAttributes = entry.knownAttributes,
                        unknownAttributes = entry.unknownAttributes,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else if (!existing.isUserEdited) {
                channelsToUpdate.add(
                    existing.copy(
                        name = entry.name,
                        tvgLogo = entry.tvgLogo ?: existing.tvgLogo,
                        streamUrl = entry.streamUrl,
                        updatedAt = now
                    )
                )
            }
        }

        if (channelsToInsert.isNotEmpty()) {
            channelsToInsert.chunked(500).forEach { channelDao.insertChannels(it) }
        }
        if (channelsToUpdate.isNotEmpty()) {
            channelsToUpdate.chunked(500).forEach { channelDao.updateChannels(it) }
        }

        return Result.success(playlistId)
    }

    // Playlist CRUD
    suspend fun renamePlaylist(id: Long, newName: String) = withContext(Dispatchers.IO) {
        playlistDao.renamePlaylist(id, newName.trim())
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistById(id)
    }

    suspend fun setActivePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.setActivePlaylist(id)
    }

    suspend fun duplicatePlaylist(id: Long): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val original = playlistDao.getPlaylistById(id) ?: return@withContext Result.failure(Exception("Playlist not found"))
            val now = System.currentTimeMillis()
            val newPlaylist = original.copy(
                id = 0,
                name = "${original.name} (Copy)",
                createdAt = now,
                updatedAt = now,
                isActive = false
            )
            val newId = playlistDao.insertPlaylist(newPlaylist)

            val categories = categoryDao.getCategoriesForPlaylist(id)
            val categoryIdMap = mutableMapOf<Long, Long>()
            val newCategories = categories.map { cat ->
                val newCat = cat.copy(id = 0, playlistId = newId)
                val createdId = categoryDao.insertCategory(newCat)
                categoryIdMap[cat.id] = createdId
                newCat
            }

            val channels = channelDao.getAllChannelsForPlaylist(id)
            val newChannels = channels.map { ch ->
                ch.copy(
                    id = 0,
                    playlistId = newId,
                    categoryId = ch.categoryId?.let { categoryIdMap[it] },
                    createdAt = now,
                    updatedAt = now
                )
            }
            newChannels.chunked(500).forEach { channelDao.insertChannels(it) }

            Result.success(newId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Category CRUD
    suspend fun addCategory(playlistId: Long, name: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return@withContext Result.failure(Exception("Category name cannot be empty"))
            val existing = categoryDao.getCategoryByName(playlistId, trimmed)
            if (existing != null) return@withContext Result.failure(Exception("Category already exists"))

            val categories = categoryDao.getCategoriesForPlaylist(playlistId)
            val category = CategoryEntity(
                playlistId = playlistId,
                name = trimmed,
                position = categories.size,
                isVisible = true
            )
            val id = categoryDao.insertCategory(category)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameCategory(playlistId: Long, categoryId: Long, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val trimmed = newName.trim()
            if (trimmed.isEmpty()) return@withContext Result.failure(Exception("Category name cannot be empty"))

            val current = categoryDao.getCategoriesForPlaylist(playlistId).find { it.id == categoryId }
                ?: return@withContext Result.failure(Exception("Category not found"))

            categoryDao.renameCategory(categoryId, trimmed)
            channelDao.mergeChannelsCategory(playlistId, current.name, categoryId, trimmed)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun mergeCategories(playlistId: Long, fromCategoryName: String, toCategoryName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val destCat = categoryDao.getCategoryByName(playlistId, toCategoryName)
            val destCatId = destCat?.id

            channelDao.mergeChannelsCategory(playlistId, fromCategoryName, destCatId, toCategoryName)
            val fromCat = categoryDao.getCategoryByName(playlistId, fromCategoryName)
            if (fromCat != null && fromCategoryName != toCategoryName) {
                categoryDao.deleteCategoryById(fromCat.id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(playlistId: Long, categoryId: Long, deleteChannels: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val category = categoryDao.getCategoriesForPlaylist(playlistId).find { it.id == categoryId }
                ?: return@withContext Result.failure(Exception("Category not found"))

            if (deleteChannels) {
                val channels = channelDao.getAllChannelsForPlaylist(playlistId).filter { it.categoryName == category.name }
                channelDao.deleteChannelsByIds(channels.map { it.id })
            } else {
                channelDao.mergeChannelsCategory(playlistId, category.name, null, "Uncategorized")
            }
            categoryDao.deleteCategoryById(categoryId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reorderCategories(playlistId: Long, categories: List<PlaylistCategory>) = withContext(Dispatchers.IO) {
        val entities = categories.mapIndexed { index, cat ->
            CategoryEntity(
                id = cat.id,
                playlistId = playlistId,
                name = cat.name,
                position = index,
                isVisible = cat.isVisible
            )
        }
        categoryDao.updateCategories(entities)
    }

    // Channel CRUD
    suspend fun saveChannel(channel: PlaylistChannel): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val entity = ChannelEntity.fromDomain(channel.copy(isUserEdited = true, updatedAt = now))
            if (entity.id == 0L) {
                val maxPos = (channelDao.getMaxPosition(channel.playlistId) ?: -1) + 1
                val id = channelDao.insertChannel(entity.copy(position = maxPos))
                Result.success(id)
            } else {
                channelDao.updateChannel(entity)
                Result.success(entity.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun duplicateChannel(channelId: Long): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val original = channelDao.getChannelById(channelId) ?: return@withContext Result.failure(Exception("Channel not found"))
            val now = System.currentTimeMillis()
            val newChannel = original.copy(
                id = 0,
                name = "${original.name} (Copy)",
                position = original.position + 1,
                isUserEdited = true,
                createdAt = now,
                updatedAt = now
            )
            val id = channelDao.insertChannel(newChannel)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChannel(channelId: Long) = withContext(Dispatchers.IO) {
        channelDao.deleteChannelById(channelId)
    }

    suspend fun bulkDeleteChannels(channelIds: List<Long>) = withContext(Dispatchers.IO) {
        channelDao.deleteChannelsByIds(channelIds)
    }

    suspend fun moveChannelCategory(channelId: Long, newCategoryId: Long?, newCategoryName: String) = withContext(Dispatchers.IO) {
        channelDao.updateChannelCategory(channelId, newCategoryId, newCategoryName)
    }

    suspend fun bulkMoveCategory(channelIds: List<Long>, newCategoryId: Long?, newCategoryName: String) = withContext(Dispatchers.IO) {
        channelDao.bulkUpdateCategory(channelIds, newCategoryId, newCategoryName)
    }

    suspend fun reorderChannels(channels: List<PlaylistChannel>) = withContext(Dispatchers.IO) {
        val updated = channels.mapIndexed { index, ch ->
            ChannelEntity.fromDomain(ch.copy(position = index, isUserEdited = true))
        }
        channelDao.updateChannels(updated)
    }

    suspend fun toggleFavorite(channelId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        channelDao.toggleFavorite(channelId, isFavorite)
    }

    // Exporting
    suspend fun exportPlaylistToString(
        playlistId: Long,
        options: M3uExportOptions = M3uExportOptions(),
        categoryFilter: String? = null,
        selectedChannelIds: List<Long>? = null
    ): String = withContext(Dispatchers.IO) {
        val playlist = getPlaylist(playlistId)
        val allChannels = channelDao.getAllChannelsForPlaylist(playlistId).map { it.toDomain() }

        val channelsToExport = when {
            selectedChannelIds != null && selectedChannelIds.isNotEmpty() -> {
                allChannels.filter { selectedChannelIds.contains(it.id) }
            }
            categoryFilter != null && categoryFilter.isNotBlank() && categoryFilter != "All" -> {
                allChannels.filter { it.categoryName == categoryFilter }
            }
            else -> allChannels
        }

        M3uExporter.exportToString(playlist, channelsToExport, options)
    }

    suspend fun exportPlaylistToStream(
        outputStream: OutputStream,
        playlistId: Long,
        options: M3uExportOptions = M3uExportOptions(),
        categoryFilter: String? = null,
        selectedChannelIds: List<Long>? = null
    ) = withContext(Dispatchers.IO) {
        val playlist = getPlaylist(playlistId)
        val allChannels = channelDao.getAllChannelsForPlaylist(playlistId).map { it.toDomain() }

        val channelsToExport = when {
            selectedChannelIds != null && selectedChannelIds.isNotEmpty() -> {
                allChannels.filter { selectedChannelIds.contains(it.id) }
            }
            categoryFilter != null && categoryFilter.isNotBlank() && categoryFilter != "All" -> {
                allChannels.filter { it.categoryName == categoryFilter }
            }
            else -> allChannels
        }

        M3uExporter.exportToStream(outputStream, playlist, channelsToExport, options)
    }

    private fun buildChannelMatchingKey(tvgId: String?, streamUrl: String, name: String, category: String): String {
        return if (!tvgId.isNullOrBlank()) {
            "tvgid:${tvgId.trim().lowercase()}"
        } else if (streamUrl.isNotBlank()) {
            "url:${streamUrl.trim()}"
        } else {
            "name:${category.trim().lowercase()}_${name.trim().lowercase()}"
        }
    }

    private fun extractNameFromUrl(url: String): String {
        return try {
            val clean = url.substringBefore("?").substringBefore("#")
            val segment = clean.substringAfterLast("/").substringBeforeLast(".")
            if (segment.isNotBlank()) {
                segment.replace("-", " ").replace("_", " ").capitalizeWord()
            } else {
                "Online Playlist"
            }
        } catch (_: Exception) {
            "Online Playlist"
        }
    }

    private fun String.capitalizeWord(): String {
        return split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun PlaylistWithCounts.toDomain(): Playlist {
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

    private fun CategoryWithCount.toDomain(): PlaylistCategory {
        return PlaylistCategory(
            id = id,
            playlistId = playlistId,
            name = name,
            position = position,
            isVisible = isVisible,
            channelCount = channelCount
        )
    }
}
