package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.ApkDownloader
import com.example.data.repository.UpdateRepository
import com.example.domain.model.DownloadState
import com.example.domain.model.UpdateCheckState
import com.example.domain.model.UpdateInfo
import com.example.ui.update.ApkInstallerHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UpdateRepository(application)
    private val downloader = ApkDownloader(application)

    private val _updateState = MutableStateFlow(UpdateCheckState())
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _showDownloadModal = MutableStateFlow(false)
    val showDownloadModal: StateFlow<Boolean> = _showDownloadModal.asStateFlow()

    private val _activeUpdateInfo = MutableStateFlow<UpdateInfo?>(null)
    val activeUpdateInfo: StateFlow<UpdateInfo?> = _activeUpdateInfo.asStateFlow()

    private var downloadJob: Job? = null

    init {
        if (repository.isAutoCheckEnabled()) {
            checkForUpdates(isUserInitiated = false)
        }
    }

    fun checkForUpdates(isUserInitiated: Boolean = true) {
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(
                isChecking = true,
                errorMessage = null,
                isUserInitiated = isUserInitiated
            )

            val result = repository.checkForUpdates(forceUserInitiated = isUserInitiated)
            result.fold(
                onSuccess = { updateInfo ->
                    _updateState.value = _updateState.value.copy(
                        isChecking = false,
                        updateInfo = updateInfo,
                        errorMessage = null,
                        isUserInitiated = isUserInitiated
                    )
                    _activeUpdateInfo.value = updateInfo
                    if (updateInfo.isUpdateAvailable) {
                        _showUpdateDialog.value = true
                    }
                },
                onFailure = { error ->
                    _updateState.value = _updateState.value.copy(
                        isChecking = false,
                        errorMessage = error.localizedMessage ?: "Failed to check for updates",
                        isUserInitiated = isUserInitiated
                    )
                }
            )
        }
    }

    /**
     * Triggered when user clicks "Download Now" on the Update Changelog Dialog.
     * Initiates the internal background download and shows the Downloader modal with live progress.
     */
    fun startInAppDownload(updateInfo: UpdateInfo) {
        _activeUpdateInfo.value = updateInfo
        _showUpdateDialog.value = false
        _showDownloadModal.value = true

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            downloader.downloadApk(updateInfo.downloadUrl, updateInfo.latestVersion)
                .collect { state ->
                    _downloadState.value = state
                    if (state is DownloadState.Completed) {
                        // Automatically attempt install if permission already exists
                        val appContext = getApplication<Application>()
                        if (ApkInstallerHelper.canRequestPackageInstalls(appContext)) {
                            ApkInstallerHelper.launchInstallApk(appContext, state.apkFile)
                        }
                    }
                }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (ApkInstallerHelper.canRequestPackageInstalls(context)) {
            ApkInstallerHelper.launchInstallApk(context, apkFile)
        } else {
            ApkInstallerHelper.openUnknownSourcesSettings(context)
        }
    }

    fun openUnknownSourcesSettings(context: Context) {
        ApkInstallerHelper.openUnknownSourcesSettings(context)
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = DownloadState.Idle
        _showDownloadModal.value = false
    }

    fun dismissDownloadModal() {
        if (_downloadState.value is DownloadState.Downloading) {
            cancelDownload()
        } else {
            _showDownloadModal.value = false
            _downloadState.value = DownloadState.Idle
        }
    }

    fun retryDownload(updateInfo: UpdateInfo) {
        startInAppDownload(updateInfo)
    }

    fun openBrowserFallback(updateInfo: UpdateInfo) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.htmlUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(fallbackIntent)
        }
    }

    fun onRemindLater(updateInfo: UpdateInfo) {
        repository.snoozeUpdate(updateInfo.latestVersion)
        _showUpdateDialog.value = false
    }

    fun dismissDialog() {
        _showUpdateDialog.value = false
    }

    fun clearError() {
        _updateState.value = _updateState.value.copy(errorMessage = null)
    }

    fun getConnectedRepo(): Pair<String, String> = repository.getConnectedRepo()

    fun updateConnectedRepo(owner: String, repo: String) {
        repository.setConnectedRepo(owner, repo)
    }

    fun isAutoCheckEnabled(): Boolean = repository.isAutoCheckEnabled()

    fun setAutoCheckEnabled(enabled: Boolean) {
        repository.setAutoCheckEnabled(enabled)
    }
}

