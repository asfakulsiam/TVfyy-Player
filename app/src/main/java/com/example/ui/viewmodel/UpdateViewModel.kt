package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.UpdateRepository
import com.example.domain.model.UpdateCheckState
import com.example.domain.model.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UpdateRepository(application)

    private val _updateState = MutableStateFlow(UpdateCheckState())
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

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

    fun onDownloadNow(updateInfo: UpdateInfo) {
        _showUpdateDialog.value = false
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {
            // Fallback to html release page
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
