package com.example.data.remote

import com.example.domain.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class GithubUpdateService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {

    suspend fun fetchLatestRelease(
        owner: String,
        repo: String,
        currentVersionName: String
    ): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val cleanOwner = owner.trim()
            val cleanRepo = repo.trim()
            var json: JSONObject? = null

            // 1. Try fetching latest release
            val latestUrl = "https://api.github.com/repos/$cleanOwner/$cleanRepo/releases/latest"
            val latestRequest = Request.Builder()
                .url(latestUrl)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "TVfyy-Player-Android-App")
                .build()

            val latestResponse = client.newCall(latestRequest).execute()
            if (latestResponse.isSuccessful) {
                val body = latestResponse.body?.string()
                if (!body.isNullOrBlank()) {
                    json = JSONObject(body)
                }
            } else if (latestResponse.code == 403) {
                return@withContext Result.failure(
                    Exception("GitHub API rate limit exceeded. Please try again later.")
                )
            }

            // 2. Fallback to /releases list if /releases/latest was 404 or empty
            if (json == null) {
                val listUrl = "https://api.github.com/repos/$cleanOwner/$cleanRepo/releases?per_page=1"
                val listRequest = Request.Builder()
                    .url(listUrl)
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .addHeader("User-Agent", "TVfyy-Player-Android-App")
                    .build()

                val listResponse = client.newCall(listRequest).execute()
                if (listResponse.isSuccessful) {
                    val listBody = listResponse.body?.string()
                    if (!listBody.isNullOrBlank()) {
                        val arr = org.json.JSONArray(listBody)
                        if (arr.length() > 0) {
                            json = arr.getJSONObject(0)
                        }
                    }
                }
            }

            if (json == null) {
                return@withContext Result.failure(
                    Exception("No releases found on GitHub for $cleanOwner/$cleanRepo.")
                )
            }

            val tagName = json.optString("tag_name", "").trim()
            val title = json.optString("name", tagName).trim()
            val releaseNotes = json.optString("body", "Performance improvements and bug fixes.")
            val htmlUrl = json.optString("html_url", "https://github.com/$cleanOwner/$cleanRepo/releases")
            val publishedAt = json.optString("published_at", "")

            var downloadUrl = htmlUrl
            var assetSizeFormatted: String? = null

            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null && assetsArray.length() > 0) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    val assetUrl = asset.optString("browser_download_url", "")
                    val sizeBytes = asset.optLong("size", 0L)

                    if (assetName.endsWith(".apk", ignoreCase = true) || downloadUrl == htmlUrl) {
                        downloadUrl = assetUrl
                        if (sizeBytes > 0) {
                            val sizeMb = sizeBytes / (1024.0 * 1024.0)
                            assetSizeFormatted = String.format(java.util.Locale.US, "%.1f MB", sizeMb)
                        }
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            break
                        }
                    }
                }
            }

            // Detect remote version across tag name, release title, and changelog headers
            val isUpdateAvailable = isRemoteVersionNewer(
                currentVersion = currentVersionName,
                remoteTag = tagName,
                remoteTitle = title,
                releaseNotes = releaseNotes
            )

            val displayVersion = resolveDisplayVersion(tagName, title, releaseNotes)

            val updateInfo = UpdateInfo(
                currentVersion = currentVersionName,
                latestVersion = displayVersion,
                releaseTitle = if (title.isBlank()) "TVfyy Player $displayVersion" else title,
                releaseNotes = releaseNotes,
                htmlUrl = htmlUrl,
                downloadUrl = downloadUrl,
                publishedAt = publishedAt,
                apkSizeFormatted = assetSizeFormatted,
                isUpdateAvailable = isUpdateAvailable
            )

            Result.success(updateInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolve the most accurate version tag to show to the user
     */
    fun resolveDisplayVersion(tag: String, title: String, notes: String): String {
        if (tag.isNotBlank()) return tag
        val titleMatch = Regex("""v?(\d+\.\d+(?:\.\d+)?)""").find(title)
        if (titleMatch != null) return "v${titleMatch.groupValues[1]}"
        val notesMatch = Regex("""\[(\d+\.\d+(?:\.\d+)?)\]""").find(notes)
        if (notesMatch != null) return "v${notesMatch.groupValues[1]}"
        return tag.ifBlank { "Latest" }
    }

    /**
     * Compare semantic version strings across candidate sources
     */
    fun isRemoteVersionNewer(
        currentVersion: String,
        remoteTag: String,
        remoteTitle: String = "",
        releaseNotes: String = ""
    ): Boolean {
        try {
            val currParts = parseVersionParts(currentVersion)
            if (currParts.isEmpty()) return false

            val candidateStrings = listOf(remoteTag, remoteTitle, releaseNotes.take(150))
            var bestRemoteParts: List<Int> = emptyList()

            for (candidate in candidateStrings) {
                val parts = parseVersionParts(candidate)
                if (parts.isNotEmpty()) {
                    if (bestRemoteParts.isEmpty() || isPartsGreater(parts, bestRemoteParts)) {
                        bestRemoteParts = parts
                    }
                }
            }

            if (bestRemoteParts.isEmpty()) {
                return remoteTag.isNotBlank() && !remoteTag.equals(currentVersion, ignoreCase = true)
            }

            return isPartsGreater(bestRemoteParts, currParts)
        } catch (_: Exception) {
            return remoteTag.isNotBlank() && !remoteTag.equals(currentVersion, ignoreCase = true)
        }
    }

    private fun parseVersionParts(text: String): List<Int> {
        if (text.isBlank()) return emptyList()
        val regex = Regex("""(\d+(?:\.\d+)+)""")
        val match = regex.find(text)
        if (match != null) {
            return match.groupValues[1].split(".").mapNotNull { it.toIntOrNull() }
        }
        val nums = Regex("""\d+""").findAll(text).mapNotNull { it.value.toIntOrNull() }.toList()
        return nums
    }

    private fun isPartsGreater(a: List<Int>, b: List<Int>): Boolean {
        val maxLen = maxOf(a.size, b.size)
        for (i in 0 until maxLen) {
            val partA = a.getOrElse(i) { 0 }
            val partB = b.getOrElse(i) { 0 }
            if (partA > partB) return true
            if (partA < partB) return false
        }
        return false
    }
}
