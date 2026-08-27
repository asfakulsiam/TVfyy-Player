package com.example.domain.model

import java.io.File

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

sealed interface DownloadState {
    object Idle : DownloadState

    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercent: Int, // 0..100
        val downloadedFormatted: String,
        val totalFormatted: String,
        val speedFormatted: String? = null,
        val isIndeterminate: Boolean = false
    ) : DownloadState

    data class Completed(
        val apkFile: File,
        val versionTag: String,
        val fileSizeBytes: Long
    ) : DownloadState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : DownloadState
}
