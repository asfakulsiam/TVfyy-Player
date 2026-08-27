package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class UpdatePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tvfyy_update_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SNOOZED_VERSION = "update_snoozed_version"
        private const val KEY_SNOOZE_TIMESTAMP = "update_snooze_timestamp"
        private const val KEY_LAST_CHECK_TIMESTAMP = "update_last_check_timestamp"
        private const val KEY_REPO_OWNER = "update_repo_owner"
        private const val KEY_REPO_NAME = "update_repo_name"
        private const val KEY_AUTO_CHECK = "update_auto_check_enabled"
        private const val DEFAULT_REPO_OWNER = "asfakulsiam"
        private const val DEFAULT_REPO_NAME = "TVfyy-Player"
        private const val SNOOZE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    fun getRepoOwner(): String = prefs.getString(KEY_REPO_OWNER, DEFAULT_REPO_OWNER) ?: DEFAULT_REPO_OWNER

    fun getRepoName(): String = prefs.getString(KEY_REPO_NAME, DEFAULT_REPO_NAME) ?: DEFAULT_REPO_NAME

    fun setRepoDetails(owner: String, name: String) {
        prefs.edit()
            .putString(KEY_REPO_OWNER, owner.trim())
            .putString(KEY_REPO_NAME, name.trim())
            .apply()
    }

    fun isAutoCheckEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_CHECK, true)

    fun setAutoCheckEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CHECK, enabled).apply()
    }

    fun snoozeVersion(versionTag: String) {
        prefs.edit()
            .putString(KEY_SNOOZED_VERSION, versionTag)
            .putLong(KEY_SNOOZE_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun shouldPromptForVersion(versionTag: String): Boolean {
        val snoozedVersion = prefs.getString(KEY_SNOOZED_VERSION, null) ?: return true
        if (snoozedVersion != versionTag) return true // New version available

        val snoozeTime = prefs.getLong(KEY_SNOOZE_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        return (now - snoozeTime) > SNOOZE_DURATION_MS
    }

    fun recordCheckTimestamp() {
        prefs.edit().putLong(KEY_LAST_CHECK_TIMESTAMP, System.currentTimeMillis()).apply()
    }

    fun getLastCheckTimestamp(): Long = prefs.getLong(KEY_LAST_CHECK_TIMESTAMP, 0L)
}
