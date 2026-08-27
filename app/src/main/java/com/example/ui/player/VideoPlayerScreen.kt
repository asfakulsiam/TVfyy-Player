package com.example.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.example.domain.model.MediaItemData
import com.example.ui.common.FailedStreamScreen
import com.example.ui.player.components.AddSubtitleUrlDialog
import com.example.ui.player.components.AudioSettingsSheet
import com.example.ui.player.components.GestureOverlays
import com.example.ui.player.components.OnlineSubtitleSearchSheet
import com.example.ui.player.components.PlaybackSpeedSheet
import com.example.ui.player.components.PlayerControls
import com.example.ui.player.components.QualitySelectionSheet
import com.example.ui.player.components.QueueSheet
import com.example.ui.player.components.ResumePlaybackDialog
import com.example.ui.player.components.StreamInfoDialog
import com.example.ui.player.components.SubtitleOverlayView
import com.example.ui.player.components.SubtitleSettingsSheet
import com.example.ui.player.components.SubtitleStyleSheet
import com.example.ui.support.ShortsSupportModal
import com.example.ui.theme.AccentError
import com.example.ui.theme.CyanPrimary
import com.example.ui.viewmodel.PlayerViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    mediaItemData: MediaItemData,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onOpenSupport: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val videoTracks by viewModel.videoTracks.collectAsStateWithLifecycle()
    val audioTracks by viewModel.audioTracks.collectAsStateWithLifecycle()
    val subtitleTracks by viewModel.subtitleTracks.collectAsStateWithLifecycle()
    val streamDiagnostics by viewModel.streamDiagnostics.collectAsStateWithLifecycle()
    val activeCuesText by viewModel.activeCuesText.collectAsStateWithLifecycle()

    var systemBrightness by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(mediaItemData) {
        viewModel.initializePlayback(mediaItemData)
    }

    LaunchedEffect(uiState.requestedOrientation) {
        activity?.requestedOrientation = uiState.requestedOrientation
    }

    // Keep screen on during playback
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_screen")
    ) {
        // Native ExoPlayer View with Pinch-to-Zoom transform support
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    subtitleView?.visibility = android.view.View.GONE // Render using custom Compose SubtitleOverlayView
                    resizeMode = uiState.currentResizeMode.mode
                    player = viewModel.getPlayer()
                }
            },
            update = { playerView ->
                playerView.subtitleView?.visibility = android.view.View.GONE
                playerView.resizeMode = uiState.currentResizeMode.mode
                playerView.player = viewModel.getPlayer()
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = uiState.zoomScale,
                    scaleY = uiState.zoomScale,
                    translationX = uiState.zoomPanX,
                    translationY = uiState.zoomPanY
                )
        )

        // Custom Subtitle Overlay with rich typography, Bengali script support, and vertical drag
        SubtitleOverlayView(
            activeCuesText = activeCuesText,
            subtitleStyle = uiState.subtitleStyle,
            onAdjustVerticalOffset = { viewModel.adjustSubtitleVerticalOffset(it) }
        )

        // Gesture Touch Input Overlay Layer (active when not in PiP)
        if (!uiState.isInPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(uiState.isControlsLocked) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (!uiState.isControlsLocked && zoom != 1f) {
                                viewModel.onPinchZoom(zoom, pan.x, pan.y)
                            }
                        }
                    }
                    .pointerInput(uiState.isControlsLocked, uiState.showControls) {
                        detectTapGestures(
                            onTap = {
                                viewModel.toggleControls()
                            },
                            onDoubleTap = { offset ->
                                if (!uiState.isControlsLocked) {
                                    val isRightSide = offset.x > size.width / 2
                                    viewModel.onDoubleTapSeek(isRightSide)
                                }
                            },
                            onLongPress = {
                                if (!uiState.isControlsLocked) {
                                    viewModel.onLongPressStart()
                                }
                            },
                            onPress = {
                                tryAwaitRelease()
                                viewModel.onLongPressEnd()
                            }
                        )
                    }
                    .pointerInput(uiState.isControlsLocked, uiState.showControls) {
                        // Only enable fullscreen swipe gestures when controls are hidden
                        if (uiState.isControlsLocked || uiState.showControls) return@pointerInput
                        var totalDragX = 0f
                        var totalDragY = 0f
                        var isLeftHalf = false
                        var isHorizontalDrag = false
                        var isVerticalDrag = false

                        detectDragGestures(
                            onDragStart = { startOffset ->
                                totalDragX = 0f
                                totalDragY = 0f
                                isLeftHalf = startOffset.x < size.width / 2
                                isHorizontalDrag = false
                                isVerticalDrag = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y

                                if (!isHorizontalDrag && !isVerticalDrag) {
                                    if (abs(totalDragX) > 20f && abs(totalDragX) > abs(totalDragY)) {
                                        isHorizontalDrag = true
                                    } else if (abs(totalDragY) > 20f && abs(totalDragY) > abs(totalDragX)) {
                                        isVerticalDrag = true
                                    }
                                }

                                if (isHorizontalDrag) {
                                    val seekDelta = (totalDragX * 120).toLong()
                                    viewModel.onSeekGesture(seekDelta)
                                } else if (isVerticalDrag) {
                                    val deltaPercent = -dragAmount.y / (size.height * 0.7f)
                                    if (isLeftHalf) {
                                        systemBrightness = (systemBrightness + deltaPercent).coerceIn(0.01f, 1.0f)
                                        activity?.window?.attributes = activity?.window?.attributes?.apply {
                                            screenBrightness = systemBrightness
                                        }
                                        viewModel.onBrightnessGesture(systemBrightness)
                                    } else {
                                        val currentVol = playbackState.volume
                                        val targetVol = (currentVol + deltaPercent).coerceIn(0.0f, 1.0f)
                                        viewModel.onVolumeGesture(targetVol)
                                    }
                                }
                            },
                            onDragEnd = {
                                if (isHorizontalDrag) {
                                    viewModel.onSeekGestureFinished()
                                }
                            },
                            onDragCancel = {
                                if (isHorizontalDrag) {
                                    viewModel.onSeekGestureFinished()
                                }
                            }
                        )
                    }
            )
        }

        // Gesture HUD Overlays (Brightness / Volume / Seek / Double-Tap / 2X / Zoom / Reconnect)
        if (!uiState.isInPipMode) {
            GestureOverlays(
                activeGesture = uiState.activeGesture,
                brightness = uiState.gestureBrightness,
                volume = uiState.gestureVolume,
                seekDeltaMs = uiState.gestureSeekDeltaMs,
                seekTargetMs = uiState.gestureSeekTargetMs,
                totalDurationMs = playbackState.durationMs,
                doubleTapSeekSide = uiState.doubleTapSeekSide,
                doubleTapSeekSeconds = uiState.playerSettings.doubleTapSeekSeconds,
                isHoldingSpeedUp = uiState.isHoldingSpeedUp,
                zoomScale = uiState.zoomScale,
                onResetZoom = { viewModel.resetZoom() },
                isReconnecting = playbackState.isReconnecting,
                reconnectAttempt = playbackState.reconnectAttempt
            )
        }

        // Floating Status Notification Toast
        AnimatedVisibility(
            visible = uiState.userNotificationMessage != null,
            enter = fadeIn() + slideInVertically { -40 },
            exit = fadeOut() + slideOutVertically { -40 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xDD1E1E1E)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.userNotificationMessage ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Error Screen Overlay (with rich diagnostics, hardware fallback, network check & retry)
        AnimatedVisibility(
            visible = playbackState.errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FailedStreamScreen(
                errorMessage = playbackState.errorMessage ?: "Unknown error occurred while playing stream.",
                streamUrl = mediaItemData.uri,
                onRetry = { viewModel.retry() },
                onBack = onBack,
                onToggleSoftwareDecoder = {
                    viewModel.retry()
                }
            )
        }

        // On-Screen Player Controls (Top Bar, Center, Bottom Timeline & Quality/Tracks)
        if (!uiState.isInPipMode && playbackState.errorMessage == null) {
            PlayerControls(
                visible = uiState.showControls,
                isLocked = uiState.isControlsLocked,
                title = uiState.currentMedia?.title ?: "TVfyy Player",
                playbackState = playbackState,
                resizeMode = uiState.currentResizeMode,
                isFavorite = uiState.isFavorite,
                hasQueue = uiState.queue.size > 1,
                onBack = onBack,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeekTo = { viewModel.seekTo(it) },
                onSeekBy = { viewModel.seekBy(it) },
                onJumpToLive = { viewModel.jumpToLiveEdge() },
                onPlayPrevious = { viewModel.playPrevious() },
                onPlayNext = { viewModel.playNext() },
                onToggleLock = { viewModel.toggleLockControls() },
                onToggleFavorite = { viewModel.toggleFavorite() },
                onToggleOrientation = { viewModel.toggleOrientationMode() },
                onOpenPip = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            val aspect = if (playbackState.videoWidth > 0 && playbackState.videoHeight > 0) {
                                Rational(playbackState.videoWidth, playbackState.videoHeight)
                            } else {
                                Rational(16, 9)
                            }
                            val pipParams = PictureInPictureParams.Builder()
                                .setAspectRatio(aspect)
                                .build()
                            activity?.enterPictureInPictureMode(pipParams)
                            viewModel.setInPipMode(true)
                        } catch (_: Exception) {}
                    }
                },
                onOpenQueue = { viewModel.showQueueSheet(true) },
                onOpenQuality = { viewModel.showQualitySheet(true) },
                onOpenAudio = { viewModel.showAudioSheet(true) },
                onOpenSubtitle = { viewModel.showSubtitleSheet(true) },
                onOpenSpeed = { viewModel.showSpeedSheet(true) },
                onOpenDiagnostics = { viewModel.showDiagnosticsSheet(true) },
                onCycleResize = { viewModel.cycleResizeMode() }
            )
        }

        // Resume Playback Dialog
        if (uiState.showResumeDialog) {
            ResumePlaybackDialog(
                resumePositionMs = uiState.resumePromptPositionMs,
                onResume = { viewModel.resumePlayback() },
                onStartOver = { viewModel.startOverPlayback() },
                onDismiss = { viewModel.dismissResumeDialog() }
            )
        }

        // Modal Sheets
        if (uiState.showQualitySheet) {
            QualitySelectionSheet(
                videoTracks = videoTracks,
                onSelectQuality = { viewModel.selectVideoTrack(it) },
                onDismiss = { viewModel.showQualitySheet(false) }
            )
        }

        if (uiState.showAudioSheet) {
            AudioSettingsSheet(
                audioTracks = audioTracks,
                audioDelayMs = playbackState.audioDelayMs,
                volumeBoostPercent = playbackState.volumeBoostPercent,
                onSelectAudio = { viewModel.selectAudioTrack(it) },
                onChangeAudioDelay = { viewModel.setAudioDelay(it) },
                onChangeVolumeBoost = { viewModel.setVolumeBoost(it) },
                onDismiss = { viewModel.showAudioSheet(false) }
            )
        }

        if (uiState.showSubtitleSheet) {
            SubtitleSettingsSheet(
                subtitleTracks = subtitleTracks,
                subtitleDelayMs = playbackState.subtitleDelayMs,
                onSelectSubtitle = { viewModel.selectSubtitleTrack(it) },
                onChangeSubtitleDelay = { viewModel.setSubtitleDelay(it) },
                onLoadExternalSubtitle = { viewModel.loadExternalSubtitle(it) },
                onOpenOnlineSearch = { viewModel.showOnlineSubtitleSheet(true) },
                onOpenAddUrl = { viewModel.showSubtitleUrlDialog(true) },
                onOpenStyleSettings = { viewModel.showSubtitleStyleSheet(true) },
                onDismiss = { viewModel.showSubtitleSheet(false) }
            )
        }

        if (uiState.showOnlineSubtitleSheet) {
            OnlineSubtitleSearchSheet(
                initialTitle = uiState.parsedTitle.ifBlank { uiState.currentMedia?.title ?: "" },
                subtitleManager = viewModel.subtitleManager,
                onSubtitleDownloaded = { viewModel.onSubtitleDownloaded(it) },
                onDismiss = { viewModel.showOnlineSubtitleSheet(false) }
            )
        }

        if (uiState.showSubtitleUrlDialog) {
            AddSubtitleUrlDialog(
                onLoadUrl = { viewModel.loadSubtitleUrl(it) },
                onDismiss = { viewModel.showSubtitleUrlDialog(false) }
            )
        }

        if (uiState.showSubtitleStyleSheet) {
            SubtitleStyleSheet(
                styleConfig = uiState.subtitleStyle,
                onUpdateStyle = { viewModel.updateSubtitleStyle(it) },
                onDismiss = { viewModel.showSubtitleStyleSheet(false) }
            )
        }

        if (uiState.showSpeedSheet) {
            PlaybackSpeedSheet(
                currentSpeed = playbackState.playbackSpeed,
                onSelectSpeed = { viewModel.setPlaybackSpeed(it) },
                onDismiss = { viewModel.showSpeedSheet(false) }
            )
        }

        if (uiState.showQueueSheet) {
            QueueSheet(
                queue = uiState.queue,
                currentIndex = uiState.queueIndex,
                onPlayQueueIndex = { viewModel.playQueueIndex(it) },
                onRemoveFromQueue = { viewModel.removeFromQueue(it) },
                onDismiss = { viewModel.showQueueSheet(false) }
            )
        }

        if (uiState.showDiagnosticsSheet) {
            StreamInfoDialog(
                diagnostics = streamDiagnostics,
                onDismiss = { viewModel.showDiagnosticsSheet(false) }
            )
        }

        if (uiState.showSupportModal) {
            ShortsSupportModal(
                onOpenSupport = {
                    viewModel.dismissSupportModal()
                    onOpenSupport()
                },
                onDismiss = {
                    viewModel.dismissSupportModal()
                },
                onNeverShowAgain = {
                    viewModel.neverShowSupportModalAgain()
                }
            )
        }
    }
}
