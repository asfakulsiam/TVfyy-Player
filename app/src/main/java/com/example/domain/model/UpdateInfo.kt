package com.example.domain.model

data class UpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val downloadUrl: String,
    val publishedAt: String,
    val apkSizeFormatted: String?,
    val isUpdateAvailable: Boolean
)

data class UpdateCheckState(
    val isChecking: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val errorMessage: String? = null,
    val isUserInitiated: Boolean = false
)
