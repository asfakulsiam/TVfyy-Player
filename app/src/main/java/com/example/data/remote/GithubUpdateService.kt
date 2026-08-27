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
            val url = "https://api.github.com/repos/${owner.trim()}/${repo.trim()}/releases/latest"
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "TVfyy-Player-Android-App")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                if (response.code == 404) {
                    return@withContext Result.failure(
                        Exception("Repository or release not found for $owner/$repo. Please ensure the repository is public and has a release.")
                    )
                }
                if (response.code == 403) {
                    return@withContext Result.failure(
                        Exception("GitHub API rate limit exceeded. Please try again later.")
                    )
                }
                return@withContext Result.failure(
                    Exception("Failed to check for updates (HTTP ${response.code})")
                )
            }

            val bodyString = response.body?.string() ?: return@withContext Result.failure(
                Exception("Empty response from GitHub API")
            )

            val json = JSONObject(bodyString)
            val tagName = json.optString("tag_name", "")
            val title = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "No release notes provided.")
            val htmlUrl = json.optString("html_url", "https://github.com/$owner/$repo/releases")
            val publishedAt = json.optString("published_at", "")

            var downloadUrl = htmlUrl
            var assetSizeFormatted: String? = null

            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null && assetsArray.length() > 0) {
                // Find apk asset first, or fallback to first asset
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    val assetUrl = asset.optString("browser_download_url", "")
                    val sizeBytes = asset.optLong("size", 0L)

                    if (assetName.endsWith(".apk", ignoreCase = true) || downloadUrl == htmlUrl) {
                        downloadUrl = assetUrl
                        if (sizeBytes > 0) {
                            val sizeMb = sizeBytes / (1024.0 * 1024.0)
                            assetSizeFormatted = String.format("%.1f MB", sizeMb)
                        }
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            break
                        }
                    }
                }
            }

            val isUpdateAvailable = isRemoteVersionNewer(currentVersionName, tagName)

            val updateInfo = UpdateInfo(
                currentVersion = currentVersionName,
                latestVersion = tagName,
                releaseTitle = if (title.isBlank()) "TVfyy Player $tagName" else title,
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
     * Compare semantic version strings (e.g. "1.0", "v1.2.0", "1.2.1-beta")
     */
    fun isRemoteVersionNewer(currentVersion: String, remoteTag: String): Boolean {
        try {
            val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")
            val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")

            if (cleanCurrent.equals(cleanRemote, ignoreCase = true)) {
                return false
            }

            val currentParts = cleanCurrent.split(".", "-").mapNotNull { it.toIntOrNull() }
            val remoteParts = cleanRemote.split(".", "-").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(currentParts.size, remoteParts.size)
            for (i in 0 until maxLen) {
                val curr = currentParts.getOrElse(i) { 0 }
                val rem = remoteParts.getOrElse(i) { 0 }
                if (rem > curr) return true
                if (rem < curr) return false
            }
            return false
        } catch (_: Exception) {
            return remoteTag.isNotBlank() && !remoteTag.equals(currentVersion, ignoreCase = true)
        }
    }
}
