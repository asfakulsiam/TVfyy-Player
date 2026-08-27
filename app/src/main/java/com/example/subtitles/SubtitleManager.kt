package com.example.subtitles

import android.content.Context
import android.net.Uri
import com.example.domain.model.OnlineSubtitleItem
import com.example.domain.model.SubtitleSearchQuery
import com.example.domain.model.SubtitleSource
import com.example.domain.model.TrackInfo
import com.example.domain.model.TrackType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class SubtitleManager(
    private val context: Context,
    private val subtitleProvider: SubtitleProvider = CommunitySubtitleProvider()
) {

    val cacheDir: File
        get() = File(context.cacheDir, "subtitles").apply { if (!exists()) mkdirs() }

    suspend fun searchOnline(query: SubtitleSearchQuery): Result<List<OnlineSubtitleItem>> {
        return subtitleProvider.searchSubtitles(query)
    }

    suspend fun downloadOnlineSubtitle(
        item: OnlineSubtitleItem,
        onProgress: (Float) -> Unit
    ): Result<TrackInfo> = withContext(Dispatchers.IO) {
        try {
            val extension = if (item.format.equals("VTT", ignoreCase = true)) "vtt" else "srt"
            val sanitizedName = item.title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val targetFile = File(cacheDir, "${sanitizedName}_${item.languageCode}_${System.currentTimeMillis()}.$extension")

            val downloadResult = subtitleProvider.downloadSubtitle(item, targetFile, onProgress)
            if (downloadResult.isFailure) {
                return@withContext Result.failure(downloadResult.exceptionOrNull() ?: Exception("Download failed"))
            }

            val savedFile = downloadResult.getOrThrow()
            val trackInfo = TrackInfo(
                id = "online_${item.id}_${System.currentTimeMillis()}",
                trackGroupIndex = -1,
                trackIndex = -1,
                type = TrackType.SUBTITLE,
                label = "${item.language} [Online: ${item.format}]",
                language = item.language,
                languageCode = item.languageCode,
                mimeType = SubtitleFileValidator.detectMimeType(savedFile.absolutePath),
                source = SubtitleSource.ONLINE_DOWNLOAD,
                formatName = item.format,
                filePath = savedFile.absolutePath,
                isSelected = true
            )

            Result.success(trackInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importExternalSubtitleFile(
        uri: Uri,
        preferredEncoding: String? = null
    ): Result<TrackInfo> = withContext(Dispatchers.IO) {
        val validation = SubtitleFileValidator.validateAndNormalizeSubtitle(
            context = context,
            uri = uri,
            outputDir = cacheDir,
            preferredEncoding = preferredEncoding
        )

        if (!validation.isValid || validation.normalizedFile == null) {
            return@withContext Result.failure(
                Exception(validation.errorMessage ?: "Failed to import subtitle file.")
            )
        }

        val file = validation.normalizedFile
        var displayName = uri.lastPathSegment ?: file.name
        if (displayName.contains("/")) {
            displayName = displayName.substringAfterLast("/")
        }
        try {
            displayName = URLDecoder.decode(displayName, StandardCharsets.UTF_8.name())
        } catch (_: Exception) {}

        val trackInfo = TrackInfo(
            id = "ext_file_${System.currentTimeMillis()}",
            trackGroupIndex = -1,
            trackIndex = -1,
            type = TrackType.SUBTITLE,
            label = "External: $displayName",
            language = "External",
            mimeType = validation.mimeType,
            source = SubtitleSource.EXTERNAL_FILE,
            formatName = validation.detectedFormat,
            filePath = file.absolutePath,
            encoding = validation.detectedEncoding,
            isSelected = true
        )

        Result.success(trackInfo)
    }

    suspend fun downloadSubtitleFromUrl(url: String): Result<TrackInfo> = withContext(Dispatchers.IO) {
        try {
            val extension = when {
                url.contains(".vtt", ignoreCase = true) -> "vtt"
                url.contains(".ass", ignoreCase = true) -> "ass"
                else -> "srt"
            }
            val targetFile = File(cacheDir, "url_sub_${System.currentTimeMillis()}.$extension")
            val item = OnlineSubtitleItem(
                id = "custom_url",
                title = "Remote Subtitle",
                language = "Remote",
                languageCode = "und",
                format = extension.uppercase(),
                downloadUrl = url,
                releaseName = url.substringAfterLast("/").substringBefore("?")
            )

            val downloadResult = subtitleProvider.downloadSubtitle(item, targetFile) {}
            if (downloadResult.isFailure) {
                return@withContext Result.failure(downloadResult.exceptionOrNull() ?: Exception("Failed to fetch subtitle URL"))
            }

            val savedFile = downloadResult.getOrThrow()
            val trackInfo = TrackInfo(
                id = "url_${System.currentTimeMillis()}",
                trackGroupIndex = -1,
                trackIndex = -1,
                type = TrackType.SUBTITLE,
                label = "URL: ${item.releaseName.ifBlank { "Custom Subtitle" }}",
                language = "Remote",
                mimeType = SubtitleFileValidator.detectMimeType(savedFile.absolutePath),
                source = SubtitleSource.EXTERNAL_URL,
                formatName = extension.uppercase(),
                filePath = savedFile.absolutePath,
                isSelected = true
            )

            Result.success(trackInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCacheSizeFormatted(): String {
        return try {
            val files = cacheDir.listFiles() ?: emptyArray()
            val totalBytes = files.sumOf { it.length() }
            if (totalBytes < 1024) "$totalBytes B (${files.size} items)"
            else if (totalBytes < 1024 * 1024) "${totalBytes / 1024} KB (${files.size} items)"
            else String.format(java.util.Locale.US, "%.1f MB (%d items)", totalBytes / (1024f * 1024f), files.size)
        } catch (_: Exception) {
            "0 KB (0 items)"
        }
    }

    fun clearSubtitleCache(): Int {
        var count = 0
        try {
            val files = cacheDir.listFiles() ?: emptyArray()
            for (file in files) {
                if (file.delete()) count++
            }
        } catch (_: Exception) {}
        return count
    }
}
