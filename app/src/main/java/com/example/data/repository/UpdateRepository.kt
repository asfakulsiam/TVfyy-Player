package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.UpdatePreferences
import com.example.data.remote.GithubUpdateService
import com.example.domain.model.UpdateInfo

class UpdateRepository(
    context: Context,
    private val service: GithubUpdateService = GithubUpdateService(),
    private val preferences: UpdatePreferences = UpdatePreferences(context)
) {
    val prefs: UpdatePreferences = preferences

    suspend fun checkForUpdates(forceUserInitiated: Boolean = false): Result<UpdateInfo> {
        val owner = preferences.getRepoOwner()
        val repo = preferences.getRepoName()
        val currentVersion = BuildConfig.VERSION_NAME

        preferences.recordCheckTimestamp()

        val result = service.fetchLatestRelease(owner, repo, currentVersion)
        return result.map { updateInfo ->
            if (!forceUserInitiated && updateInfo.isUpdateAvailable) {
                val shouldShow = preferences.shouldPromptForVersion(updateInfo.latestVersion)
                updateInfo.copy(isUpdateAvailable = shouldShow)
            } else {
                updateInfo
            }
        }
    }

    fun snoozeUpdate(versionTag: String) {
        preferences.snoozeVersion(versionTag)
    }

    fun isAutoCheckEnabled(): Boolean = preferences.isAutoCheckEnabled()

    fun setAutoCheckEnabled(enabled: Boolean) = preferences.setAutoCheckEnabled(enabled)

    fun getConnectedRepo(): Pair<String, String> {
        return Pair(preferences.getRepoOwner(), preferences.getRepoName())
    }

    fun setConnectedRepo(owner: String, repo: String) {
        preferences.setRepoDetails(owner, repo)
    }
}
