package com.example.ui.viewmodel

import android.app.Application
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.data.local.SupportPreferences
import com.example.data.local.TvFyyDatabase
import com.example.data.repository.TvFyyRepository
import com.example.domain.model.MediaItemData
import com.example.domain.model.PlaybackState
import com.example.domain.model.PlayerSettings
import com.example.domain.model.StreamDiagnostics
import com.example.domain.model.SubtitleStyleConfig
import com.example.domain.model.TrackInfo
import com.example.player.Media3PlayerEngine
import com.example.player.PlayerEngine
import com.example.subtitles.MediaTitleParser
import com.example.subtitles.SubtitleManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class GestureType {
    NONE, BRIGHTNESS, VOLUME, SEEK
}

enum class ResizeMode(val displayName: String, val mode: Int) {
    FIT("Fit Screen", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Stretch / Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Crop / Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FIXED_16_9("16:9 Aspect", AspectRatioFrameLayout.RESIZE_MODE_FIT)
}

data class PlayerUiState(
    val currentMedia: MediaItemData? = null,
    val parsedTitle: String = "",
    val queue: List<MediaItemData> = emptyList(),
    val queueIndex: Int = 0,
    val showControls: Boolean = true,
    val isControlsLocked: Boolean = false,
    val currentResizeMode: ResizeMode = ResizeMode.FIT,
    val showQualitySheet: Boolean = false,
    val showAudioSheet: Boolean = false,
    val showSubtitleSheet: Boolean = false,
    val showOnlineSubtitleSheet: Boolean = false,
    val showSubtitleUrlDialog: Boolean = false,
    val showSubtitleStyleSheet: Boolean = false,
    val showSpeedSheet: Boolean = false,
    val showQueueSheet: Boolean = false,
    val showDiagnosticsSheet: Boolean = false,
    val showResumeDialog: Boolean = false,
    val resumePromptPositionMs: Long = 0L,
    val activeGesture: GestureType = GestureType.NONE,
    val gestureBrightness: Float = 0.5f,
    val gestureVolume: Float = 1.0f,
    val gestureSeekDeltaMs: Long = 0L,
    val gestureSeekTargetMs: Long = 0L,
    val doubleTapSeekSide: Int? = null,
    val isHoldingSpeedUp: Boolean = false,
    val previousSpeedBeforeHold: Float = 1.0f,
    val zoomScale: Float = 1.0f,
    val zoomPanX: Float = 0f,
    val zoomPanY: Float = 0f,
    val requestedOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
    val isInPipMode: Boolean = false,
    val isMiniPlayerActive: Boolean = false,
    val isFavorite: Boolean = false,
    val subtitleStyle: SubtitleStyleConfig = SubtitleStyleConfig(),
    val playerSettings: PlayerSettings = PlayerSettings(),
    val userNotificationMessage: String? = null,
    val showSupportModal: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TvFyyRepository(TvFyyDatabase.getDatabase(application))
    private val playerEngine: PlayerEngine = Media3PlayerEngine(application, viewModelScope)
    val subtitleManager = SubtitleManager(application)
    val supportPreferences = SupportPreferences(application)

    private var activeWatchSeconds: Long = 0L
    private var contextualPromptChecked: Boolean = false

    val playbackState: StateFlow<PlaybackState> = playerEngine.playbackState
    val videoTracks: StateFlow<List<TrackInfo>> = playerEngine.videoTracks
    val audioTracks: StateFlow<List<TrackInfo>> = playerEngine.audioTracks
    val subtitleTracks: StateFlow<List<TrackInfo>> = playerEngine.subtitleTracks
    val streamDiagnostics: StateFlow<StreamDiagnostics> = playerEngine.streamDiagnostics
    val activeCuesText: StateFlow<String?> = playerEngine.activeCuesText

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controlsHideJob: Job? = null
    private var gestureHideJob: Job? = null
    private var historySaveJob: Job? = null
    private var notificationHideJob: Job? = null

    init {
        startHistorySaveLoop()
    }

    fun initializePlayback(mediaItemData: MediaItemData, checkResume: Boolean = true) {
        val currentQueue = if (_uiState.value.queue.isEmpty()) {
            listOf(mediaItemData)
        } else {
            if (_uiState.value.queue.none { it.uri == mediaItemData.uri }) {
                _uiState.value.queue + mediaItemData
            } else {
                _uiState.value.queue
            }
        }
        val targetIndex = currentQueue.indexOfFirst { it.uri == mediaItemData.uri }.coerceAtLeast(0)
        val parsed = MediaTitleParser.parse(mediaItemData.title.ifBlank { mediaItemData.uri })

        _uiState.value = _uiState.value.copy(
            currentMedia = mediaItemData,
            parsedTitle = parsed.cleanTitle,
            queue = currentQueue,
            queueIndex = targetIndex,
            showControls = true,
            isMiniPlayerActive = false,
            zoomScale = 1.0f,
            zoomPanX = 0f,
            zoomPanY = 0f
        )

        viewModelScope.launch {
            if (checkResume && mediaItemData.resumePositionMs <= 0) {
                val history = repository.getHistoryItem(mediaItemData.uri)
                if (history != null && history.lastPositionMs > 5000L && (history.durationMs <= 0 || history.lastPositionMs < history.durationMs - 10000L)) {
                    _uiState.value = _uiState.value.copy(
                        showResumeDialog = true,
                        resumePromptPositionMs = history.lastPositionMs
                    )
                }
            }

            // Prepare playback engine
            playerEngine.prepare(mediaItemData)
            playerEngine.setPreferredLanguages(
                _uiState.value.playerSettings.preferredAudioLanguage,
                _uiState.value.playerSettings.preferredSubtitleLanguage
            )
            scheduleControlsHide()

            // Check favorite status
            repository.isFavorite(mediaItemData.uri).collect { fav ->
                _uiState.value = _uiState.value.copy(isFavorite = fav)
            }
        }
    }

    fun resumePlayback() {
        val position = _uiState.value.resumePromptPositionMs
        _uiState.value = _uiState.value.copy(showResumeDialog = false)
        if (position > 0) {
            seekTo(position)
        }
    }

    fun startOverPlayback() {
        _uiState.value = _uiState.value.copy(showResumeDialog = false)
        seekTo(0L)
    }

    fun dismissResumeDialog() {
        _uiState.value = _uiState.value.copy(showResumeDialog = false)
    }

    fun togglePlayPause() {
        if (playbackState.value.isPlaying) {
            playerEngine.pause()
        } else {
            playerEngine.play()
        }
        scheduleControlsHide()
    }

    fun seekTo(positionMs: Long) {
        playerEngine.seekTo(positionMs)
        scheduleControlsHide()
    }

    fun seekBy(deltaMs: Long) {
        playerEngine.seekBy(deltaMs)
        scheduleControlsHide()
    }

    fun jumpToLiveEdge() {
        playerEngine.jumpToLiveEdge()
        scheduleControlsHide()
    }

    fun onDoubleTapSeek(isForward: Boolean) {
        val delta = if (isForward) _uiState.value.playerSettings.doubleTapSeekSeconds * 1000L else -(_uiState.value.playerSettings.doubleTapSeekSeconds * 1000L)
        seekBy(delta)
        _uiState.value = _uiState.value.copy(doubleTapSeekSide = if (isForward) 1 else -1)
        viewModelScope.launch {
            delay(800)
            _uiState.value = _uiState.value.copy(doubleTapSeekSide = null)
        }
    }

    fun onLongPressStart() {
        if (!_uiState.value.isHoldingSpeedUp && playbackState.value.isPlaying) {
            val currentSpeed = playbackState.value.playbackSpeed
            _uiState.value = _uiState.value.copy(
                isHoldingSpeedUp = true,
                previousSpeedBeforeHold = currentSpeed
            )
            playerEngine.setPlaybackSpeed(2.0f)
        }
    }

    fun onLongPressEnd() {
        if (_uiState.value.isHoldingSpeedUp) {
            val restoreSpeed = _uiState.value.previousSpeedBeforeHold
            _uiState.value = _uiState.value.copy(isHoldingSpeedUp = false)
            playerEngine.setPlaybackSpeed(restoreSpeed)
        }
    }

    fun onPinchZoom(scaleChange: Float, panX: Float, panY: Float) {
        val newScale = (_uiState.value.zoomScale * scaleChange).coerceIn(1.0f, 4.0f)
        val newPanX = if (newScale > 1.0f) _uiState.value.zoomPanX + panX else 0f
        val newPanY = if (newScale > 1.0f) _uiState.value.zoomPanY + panY else 0f
        _uiState.value = _uiState.value.copy(
            zoomScale = newScale,
            zoomPanX = newPanX,
            zoomPanY = newPanY
        )
    }

    fun resetZoom() {
        _uiState.value = _uiState.value.copy(
            zoomScale = 1.0f,
            zoomPanX = 0f,
            zoomPanY = 0f
        )
    }

    fun toggleOrientationMode() {
        val next = when (_uiState.value.requestedOrientation) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        _uiState.value = _uiState.value.copy(requestedOrientation = next)
        scheduleControlsHide()
    }

    fun setPlaybackSpeed(speed: Float) {
        playerEngine.setPlaybackSpeed(speed)
        _uiState.value = _uiState.value.copy(showSpeedSheet = false)
        scheduleControlsHide()
    }

    fun setAudioDelay(delayMs: Long) {
        playerEngine.setAudioDelay(delayMs)
    }

    fun setSubtitleDelay(delayMs: Long) {
        playerEngine.setSubtitleDelay(delayMs)
    }

    fun setVolumeBoost(percent: Int) {
        playerEngine.setVolumeBoost(percent)
    }

    fun selectVideoTrack(trackInfo: TrackInfo?) {
        playerEngine.selectTrack(trackInfo)
        _uiState.value = _uiState.value.copy(showQualitySheet = false)
        scheduleControlsHide()
    }

    fun selectAudioTrack(trackInfo: TrackInfo) {
        playerEngine.selectTrack(trackInfo)
        _uiState.value = _uiState.value.copy(showAudioSheet = false)
        scheduleControlsHide()
        showNotification("Switched audio track to: ${trackInfo.language ?: trackInfo.label}")
    }

    fun selectSubtitleTrack(trackInfo: TrackInfo?) {
        playerEngine.selectSubtitleTrack(trackInfo)
        _uiState.value = _uiState.value.copy(showSubtitleSheet = false)
        scheduleControlsHide()
        if (trackInfo != null) {
            showNotification("Subtitles enabled: ${trackInfo.language ?: trackInfo.label}")
        } else {
            showNotification("Subtitles turned off")
        }
    }

    fun loadExternalSubtitle(uri: Uri) {
        viewModelScope.launch {
            val result = subtitleManager.importExternalSubtitleFile(uri, _uiState.value.subtitleStyle.encoding)
            if (result.isSuccess) {
                val track = result.getOrNull()
                if (track != null) {
                    playerEngine.addExternalSubtitleTrack(track)
                    showNotification("External subtitle loaded: ${track.label}")
                }
            } else {
                showNotification("Could not load subtitle: ${result.exceptionOrNull()?.localizedMessage}")
            }
            _uiState.value = _uiState.value.copy(showSubtitleSheet = false)
        }
    }

    fun loadSubtitleUrl(url: String) {
        _uiState.value = _uiState.value.copy(showSubtitleUrlDialog = false)
        viewModelScope.launch {
            val result = subtitleManager.downloadSubtitleFromUrl(url)
            if (result.isSuccess) {
                val track = result.getOrNull()
                if (track != null) {
                    playerEngine.addExternalSubtitleTrack(track)
                    showNotification("Remote subtitle loaded: ${track.label}")
                }
            } else {
                showNotification("Failed to download subtitle URL: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun onSubtitleDownloaded(trackInfo: TrackInfo) {
        _uiState.value = _uiState.value.copy(showOnlineSubtitleSheet = false)
        playerEngine.addExternalSubtitleTrack(trackInfo)
        showNotification("Online subtitle applied: ${trackInfo.label}")
    }

    fun updateSubtitleStyle(config: SubtitleStyleConfig) {
        _uiState.value = _uiState.value.copy(subtitleStyle = config)
    }

    fun adjustSubtitleVerticalOffset(offset: Float) {
        val updated = _uiState.value.subtitleStyle.copy(customVerticalOffsetPercent = offset)
        _uiState.value = _uiState.value.copy(subtitleStyle = updated)
    }

    fun clearSubtitleCache(): Int {
        val count = subtitleManager.clearSubtitleCache()
        showNotification("Cleared $count cached subtitle files.")
        return count
    }

    fun getSubtitleCacheStats(): String = subtitleManager.getCacheSizeFormatted()

    private fun showNotification(msg: String) {
        _uiState.value = _uiState.value.copy(userNotificationMessage = msg)
        notificationHideJob?.cancel()
        notificationHideJob = viewModelScope.launch {
            delay(2500)
            _uiState.value = _uiState.value.copy(userNotificationMessage = null)
        }
    }

    // Queue Navigation
    fun playQueueIndex(index: Int) {
        val queue = _uiState.value.queue
        if (index in queue.indices) {
            _uiState.value = _uiState.value.copy(queueIndex = index, showQueueSheet = false)
            initializePlayback(queue[index], checkResume = true)
        }
    }

    fun playNext() {
        val queue = _uiState.value.queue
        val next = _uiState.value.queueIndex + 1
        if (next in queue.indices) {
            playQueueIndex(next)
        }
    }

    fun playPrevious() {
        val queue = _uiState.value.queue
        val prev = _uiState.value.queueIndex - 1
        if (prev in queue.indices) {
            playQueueIndex(prev)
        }
    }

    fun removeFromQueue(index: Int) {
        val list = _uiState.value.queue.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            val newIndex = _uiState.value.queueIndex.coerceAtMost((list.size - 1).coerceAtLeast(0))
            _uiState.value = _uiState.value.copy(queue = list, queueIndex = newIndex)
        }
    }

    fun cycleResizeMode() {
        val modes = ResizeMode.values()
        val nextIndex = (modes.indexOf(_uiState.value.currentResizeMode) + 1) % modes.size
        _uiState.value = _uiState.value.copy(currentResizeMode = modes[nextIndex])
        scheduleControlsHide()
    }

    fun toggleControls() {
        if (_uiState.value.isControlsLocked) {
            _uiState.value = _uiState.value.copy(showControls = !_uiState.value.showControls)
            return
        }
        val target = !_uiState.value.showControls
        _uiState.value = _uiState.value.copy(showControls = target)
        if (target) scheduleControlsHide()
    }

    fun toggleLockControls() {
        val locked = !_uiState.value.isControlsLocked
        _uiState.value = _uiState.value.copy(
            isControlsLocked = locked,
            showControls = true
        )
        scheduleControlsHide()
    }

    fun toggleFavorite() {
        val current = _uiState.value.currentMedia ?: return
        viewModelScope.launch {
            repository.toggleFavorite(
                title = current.title,
                url = current.uri,
                mediaType = current.streamType.name,
                headers = current.headers
            )
        }
    }

    fun minimizeToMiniPlayer() {
        _uiState.value = _uiState.value.copy(
            isMiniPlayerActive = true,
            showControls = false
        )
    }

    fun maximizeFromMiniPlayer() {
        _uiState.value = _uiState.value.copy(
            isMiniPlayerActive = false,
            showControls = true
        )
        scheduleControlsHide()
    }

    fun closeMiniPlayer() {
        _uiState.value = _uiState.value.copy(
            isMiniPlayerActive = false,
            currentMedia = null
        )
        playerEngine.pause()
    }

    fun showQualitySheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showQualitySheet = show)
        if (!show) scheduleControlsHide()
    }

    fun showAudioSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAudioSheet = show)
        if (!show) scheduleControlsHide()
    }

    fun showSubtitleSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSubtitleSheet = show)
        if (!show) scheduleControlsHide()
    }

    fun showOnlineSubtitleSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showOnlineSubtitleSheet = show, showSubtitleSheet = false)
    }

    fun showSubtitleUrlDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSubtitleUrlDialog = show, showSubtitleSheet = false)
    }

    fun showSubtitleStyleSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSubtitleStyleSheet = show, showSubtitleSheet = false)
    }

    fun showSpeedSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSpeedSheet = show)
        if (!show) scheduleControlsHide()
    }

    fun showQueueSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showQueueSheet = show)
        if (!show) scheduleControlsHide()
    }

    fun showDiagnosticsSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDiagnosticsSheet = show)
        if (!show) scheduleControlsHide()
    }

    fun setInPipMode(inPip: Boolean) {
        _uiState.value = _uiState.value.copy(
            isInPipMode = inPip,
            showControls = !inPip
        )
    }

    // Gesture Handlers
    fun onBrightnessGesture(brightness: Float) {
        _uiState.value = _uiState.value.copy(
            activeGesture = GestureType.BRIGHTNESS,
            gestureBrightness = brightness.coerceIn(0.01f, 1.0f)
        )
        scheduleGestureHide()
    }

    fun onVolumeGesture(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        playerEngine.setVolume(clamped)
        _uiState.value = _uiState.value.copy(
            activeGesture = GestureType.VOLUME,
            gestureVolume = clamped
        )
        scheduleGestureHide()
    }

    fun onSeekGesture(deltaMs: Long) {
        val current = playbackState.value.currentPositionMs
        val duration = playbackState.value.durationMs
        val target = (current + deltaMs).coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
        _uiState.value = _uiState.value.copy(
            activeGesture = GestureType.SEEK,
            gestureSeekDeltaMs = deltaMs,
            gestureSeekTargetMs = target
        )
        scheduleGestureHide()
    }

    fun onSeekGestureFinished() {
        if (_uiState.value.activeGesture == GestureType.SEEK) {
            seekTo(_uiState.value.gestureSeekTargetMs)
        }
        _uiState.value = _uiState.value.copy(activeGesture = GestureType.NONE)
    }

    fun retry() {
        playerEngine.retry()
    }

    fun getPlayer() = playerEngine.getPlayer()

    private fun scheduleControlsHide() {
        controlsHideJob?.cancel()
        if (_uiState.value.isControlsLocked) return
        controlsHideJob = viewModelScope.launch {
            delay(4500)
            if (playbackState.value.isPlaying) {
                _uiState.value = _uiState.value.copy(showControls = false)
            }
        }
    }

    private fun scheduleGestureHide() {
        gestureHideJob?.cancel()
        gestureHideJob = viewModelScope.launch {
            delay(1200)
            _uiState.value = _uiState.value.copy(activeGesture = GestureType.NONE)
        }
    }

    fun showSupportModal(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSupportModal = show)
    }

    fun dismissSupportModal() {
        _uiState.value = _uiState.value.copy(showSupportModal = false)
    }

    fun neverShowSupportModalAgain() {
        supportPreferences.setNeverShowAgain(true)
        _uiState.value = _uiState.value.copy(showSupportModal = false)
    }

    private fun startHistorySaveLoop() {
        historySaveJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                saveCurrentPositionToHistory()
                checkContextualSupportPrompt()
            }
        }
    }

    private fun checkContextualSupportPrompt() {
        val state = playbackState.value
        val ui = _uiState.value
        if (state.isPlaying && !state.isBuffering && !ui.isInPipMode && ui.activeGesture == GestureType.NONE && state.errorMessage == null) {
            activeWatchSeconds += 5
            if (!contextualPromptChecked && activeWatchSeconds >= 45) {
                contextualPromptChecked = true
                if (supportPreferences.canShowContextualPrompt(activeWatchSeconds)) {
                    supportPreferences.recordPromptShown()
                    _uiState.value = _uiState.value.copy(showSupportModal = true)
                }
            }
        }
    }

    private suspend fun saveCurrentPositionToHistory() {
        val current = _uiState.value.currentMedia ?: return
        val state = playbackState.value
        if (state.currentPositionMs > 1000) {
            repository.saveHistory(
                title = current.title,
                url = current.uri,
                durationMs = state.durationMs,
                lastPositionMs = state.currentPositionMs,
                mediaType = current.streamType.name,
                isLocal = current.isLocalFile
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        historySaveJob?.cancel()
        controlsHideJob?.cancel()
        gestureHideJob?.cancel()
        notificationHideJob?.cancel()
        viewModelScope.launch {
            saveCurrentPositionToHistory()
        }
        playerEngine.release()
    }
}
