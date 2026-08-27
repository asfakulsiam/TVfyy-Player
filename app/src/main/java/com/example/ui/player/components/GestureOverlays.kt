package com.example.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.OverlayBackground
import com.example.ui.viewmodel.GestureType
import java.util.Locale
import kotlin.math.abs

@Composable
fun GestureOverlays(
    activeGesture: GestureType,
    brightness: Float,
    volume: Float,
    seekDeltaMs: Long,
    seekTargetMs: Long,
    totalDurationMs: Long,
    doubleTapSeekSide: Int?,
    doubleTapSeekSeconds: Int = 10,
    isHoldingSpeedUp: Boolean = false,
    zoomScale: Float = 1.0f,
    onResetZoom: () -> Unit = {},
    isReconnecting: Boolean = false,
    reconnectAttempt: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Reconnecting Network Notification
        AnimatedVisibility(
            visible = isReconnecting,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(OverlayBackground)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = CyanPrimary,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Reconnecting stream... (Attempt $reconnectAttempt/3)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Long Press Speed Up HUD (2X Fast Forward)
        AnimatedVisibility(
            visible = isHoldingSpeedUp,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(CyanPrimary)
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .testTag("speed_up_pill")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        tint = Color(0xFF00363D),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "2.0x Fast Forward",
                        color = Color(0xFF00363D),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Double Tap Seek Ripple Indicator on left or right
        AnimatedVisibility(
            visible = doubleTapSeekSide != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(if (doubleTapSeekSide == 1) Alignment.CenterEnd else Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .clip(CircleShape)
                    .background(OverlayBackground)
                    .padding(20.dp)
                    .testTag("double_tap_seek_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (doubleTapSeekSide == 1) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (doubleTapSeekSide == 1) "+$doubleTapSeekSeconds sec" else "-$doubleTapSeekSeconds sec",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Zoom Scale Reset HUD
        AnimatedVisibility(
            visible = zoomScale > 1.05f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 80.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(OverlayBackground)
                    .clickable { onResetZoom() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("reset_zoom_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format(Locale.US, "Zoom %.1fx (Tap to reset)", zoomScale),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Center HUD for Volume / Brightness / Seek swipe
        AnimatedVisibility(
            visible = activeGesture != GestureType.NONE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(OverlayBackground)
                    .padding(horizontal = 24.dp, vertical = 18.dp)
                    .testTag("gesture_hud"),
                contentAlignment = Alignment.Center
            ) {
                when (activeGesture) {
                    GestureType.BRIGHTNESS -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrightnessMedium,
                                contentDescription = "Brightness",
                                tint = CyanPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Brightness ${(brightness * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { brightness },
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = CyanPrimary,
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    GestureType.VOLUME -> {
                        val volumeIcon = when {
                            volume == 0f -> Icons.Default.VolumeMute
                            volume < 0.5f -> Icons.Default.VolumeDown
                            else -> Icons.Default.VolumeUp
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = volumeIcon,
                                contentDescription = "Volume",
                                tint = CyanPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Volume ${(volume * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { volume },
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = CyanPrimary,
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    GestureType.SEEK -> {
                        val sign = if (seekDeltaMs >= 0) "+" else "-"
                        val deltaFormatted = TimeFormatter.formatMs(abs(seekDeltaMs))
                        val targetFormatted = TimeFormatter.formatMs(seekTargetMs)
                        val totalFormatted = TimeFormatter.formatMs(totalDurationMs)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$sign$deltaFormatted",
                                color = CyanPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$targetFormatted / $totalFormatted",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                        }
                    }

                    GestureType.NONE -> Unit
                }
            }
        }
    }
}
