package com.example.subtitles

import com.example.domain.model.OnlineSubtitleItem
import com.example.domain.model.SubtitleSearchQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class CommunitySubtitleProvider(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : SubtitleProvider {

    override val providerName: String = "OpenSubtitles Public API"

    override suspend fun searchSubtitles(query: SubtitleSearchQuery): Result<List<OnlineSubtitleItem>> = withContext(Dispatchers.IO) {
        try {
            val title = query.title.trim()
            if (title.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val langParam = if (query.language.isNotBlank() && !query.language.equals("all", ignoreCase = true)) {
                "&languages=${query.language}"
            } else ""

            val request = Request.Builder()
                .url("https://api.opensubtitles.com/api/v1/subtitles?query=$encodedTitle$langParam")
                .header("User-Agent", "TVfyyPlayer v1.0")
                .header("Accept", "application/json")
                .build()

            val response = try {
                okHttpClient.newCall(request).execute()
            } catch (e: Exception) {
                // If offline or network error, return clean empty result with message
                return@withContext Result.failure(Exception("Network error searching subtitles: ${e.localizedMessage}"))
            }

            if (!response.isSuccessful) {
                // If API requires authorization or rate-limited, return empty list gracefully
                return@withContext Result.success(emptyList())
            }

            val responseBody = response.body?.string() ?: return@withContext Result.success(emptyList())
            val json = JSONObject(responseBody)
            val dataArray = json.optJSONArray("data") ?: JSONArray()

            val results = mutableListOf<OnlineSubtitleItem>()
            for (i in 0 until dataArray.length()) {
                val itemObj = dataArray.getJSONObject(i)
                val id = itemObj.optString("id", i.toString())
                val attributes = itemObj.optJSONObject("attributes") ?: continue
                val language = attributes.optString("language", "en")
                val release = attributes.optString("release", title)
                val format = attributes.optString("format", "SRT").uppercase()
                val downloadCount = attributes.optInt("download_count", 0)
                val rating = attributes.optDouble("ratings", 0.0).toFloat()

                val files = attributes.optJSONArray("files")
                var downloadUrl = ""
                var fileName = release
                if (files != null && files.length() > 0) {
                    val fileObj = files.getJSONObject(0)
                    fileName = fileObj.optString("file_name", release)
                    val fileId = fileObj.optInt("file_id", 0)
                    if (fileId > 0) {
                        downloadUrl = "https://api.opensubtitles.com/api/v1/download/$fileId"
                    }
                }

                results.add(
                    OnlineSubtitleItem(
                        id = id,
                        title = fileName,
                        language = language.uppercase(),
                        languageCode = language.lowercase(),
                        format = format,
                        downloadUrl = downloadUrl,
                        releaseName = release,
                        downloadCount = downloadCount,
                        rating = rating,
                        provider = providerName
                    )
                )
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadSubtitle(
        item: OnlineSubtitleItem,
        destinationFile: File,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (item.downloadUrl.isBlank() || (!item.downloadUrl.startsWith("http://") && !item.downloadUrl.startsWith("https://"))) {
                return@withContext Result.failure(Exception("Invalid or missing subtitle download URL."))
            }

            val request = Request.Builder()
                .url(item.downloadUrl)
                .header("User-Agent", "TVfyyPlayer/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code} error while downloading subtitle."))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body."))
            val totalBytes = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var downloaded: Long = 0
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            onProgress(downloaded.toFloat() / totalBytes)
                        }
                    }
                }
            }

            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

