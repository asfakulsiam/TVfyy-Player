package com.example.ui.player.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.SubtitleSource
import com.example.domain.model.TrackInfo
import com.example.ui.theme.CyanPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelectionSheet(
    videoTracks: List<TrackInfo>,
    onSelectQuality: (TrackInfo?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val isAutoSelected = videoTracks.none { it.isSelected }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Video Quality",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Auto Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectQuality(null) }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
                    .testTag("quality_auto_item"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto (Adaptive Bitrate)",
                        fontSize = 16.sp,
                        color = if (isAutoSelected) CyanPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isAutoSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "Automatically adjust based on connection bandwidth",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isAutoSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = CyanPrimary
                    )
                }
            }

            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

            if (videoTracks.isEmpty()) {
                Text(
                    text = "Single direct stream (hardware default decoder)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn {
                    items(videoTracks) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectQuality(track) }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                                .testTag("quality_track_${track.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.resolutionLabel,
                                    fontSize = 16.sp,
                                    color = if (track.isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (track.bitrate > 0 || track.frameRate > 0) {
                                    val details = buildList {
                                        if (track.bitrate > 0) add("${track.bitrate / 1_000_000f} Mbps")
                                        if (track.frameRate > 0) add("${track.frameRate.toInt()} fps")
                                        if (!track.codecs.isNullOrBlank()) add(track.codecs)
                                    }.joinToString(" • ")
                                    Text(
                                        text = details,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (track.isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = CyanPrimary
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSettingsSheet(
    audioTracks: List<TrackInfo>,
    audioDelayMs: Long,
    volumeBoostPercent: Int,
    onSelectAudio: (TrackInfo) -> Unit,
    onChangeAudioDelay: (Long) -> Unit,
    onChangeVolumeBoost: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Audio & Multi-Audio Tracks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Audio Delay Synchronization Section
            Text(
                text = "Audio Delay Synchronization",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyanPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onChangeAudioDelay((audioDelayMs - 250L).coerceIn(-5000L, 5000L)) }) {
                        Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = "-250ms", tint = CyanPrimary)
                    }
                    Text(
                        text = "-250",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (audioDelayMs == 0L) "0 ms (Synced)" else "${if (audioDelayMs > 0) "+" else ""}$audioDelayMs ms",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (audioDelayMs != 0L) {
                        Text(
                            text = "Reset to 0ms",
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onChangeAudioDelay(0L) }
                                .padding(2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "+250",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = { onChangeAudioDelay((audioDelayMs + 250L).coerceIn(-5000L, 5000L)) }) {
                        Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "+250ms", tint = CyanPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Software Volume Boost
            Text(
                text = "Software Volume Boost",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyanPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(100, 125, 150).forEach { boost ->
                    val isSelected = volumeBoostPercent == boost
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onChangeVolumeBoost(boost) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$boost%",
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF00363D) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            // Audio Tracks List
            Text(
                text = "Available Audio Tracks",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyanPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (audioTracks.isEmpty()) {
                Text(
                    text = "Default hardware audio track active",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.height(180.dp)) {
                    items(audioTracks) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectAudio(track) }
                                .padding(vertical = 8.dp, horizontal = 8.dp)
                                .testTag("audio_track_${track.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = track.language ?: track.label,
                                        fontSize = 15.sp,
                                        color = if (track.isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (track.isDub) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        BadgeChip(text = "Dub")
                                    }
                                    if (track.isCommentary) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        BadgeChip(text = "Commentary")
                                    }
                                    if (track.isOriginal) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        BadgeChip(text = "Original")
                                    }
                                }

                                val details = track.audioDetailsLabel
                                if (details.isNotBlank()) {
                                    Text(
                                        text = details,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (track.isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = CyanPrimary
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsSheet(
    subtitleTracks: List<TrackInfo>,
    subtitleDelayMs: Long,
    onSelectSubtitle: (TrackInfo?) -> Unit,
    onChangeSubtitleDelay: (Long) -> Unit,
    onLoadExternalSubtitle: (Uri) -> Unit,
    onOpenOnlineSearch: () -> Unit,
    onOpenAddUrl: () -> Unit,
    onOpenStyleSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isOffSelected = subtitleTracks.none { it.isSelected }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onLoadExternalSubtitle(it) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Subtitles & Synchronization",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onOpenStyleSettings) {
                    Icon(imageVector = Icons.Default.Palette, contentDescription = "Subtitle Style", tint = CyanPrimary)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle Delay
            Text(
                text = "Subtitle Delay Synchronization",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyanPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onChangeSubtitleDelay((subtitleDelayMs - 250L).coerceIn(-5000L, 5000L)) }) {
                        Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = "-250ms", tint = CyanPrimary)
                    }
                    Text(
                        text = "-250",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (subtitleDelayMs == 0L) "0 ms (Synced)" else "${if (subtitleDelayMs > 0) "+" else ""}$subtitleDelayMs ms",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitleDelayMs != 0L) {
                        Text(
                            text = "Reset to 0ms",
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onChangeSubtitleDelay(0L) }
                                .padding(2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "+250",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = { onChangeSubtitleDelay((subtitleDelayMs + 250L).coerceIn(-5000L, 5000L)) }) {
                        Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "+250ms", tint = CyanPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle Tracks
            Text(
                text = "Subtitle Tracks",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyanPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Off Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelectSubtitle(null) }
                    .padding(vertical = 8.dp, horizontal = 8.dp)
                    .testTag("subtitle_track_off"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Off (Subtitles Disabled)",
                    fontSize = 15.sp,
                    color = if (isOffSelected) CyanPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isOffSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (isOffSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = CyanPrimary
                    )
                }
            }

            LazyColumn(modifier = Modifier.height(130.dp)) {
                items(subtitleTracks) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectSubtitle(track) }
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                            .testTag("subtitle_track_${track.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = track.language ?: track.label,
                                    fontSize = 15.sp,
                                    color = if (track.isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (track.isSDH) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BadgeChip(text = "SDH")
                                }
                                if (track.isForced) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BadgeChip(text = "Forced")
                                }
                                if (track.source == SubtitleSource.ONLINE_DOWNLOAD) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BadgeChip(text = "Online")
                                } else if (track.source == SubtitleSource.EXTERNAL_FILE) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BadgeChip(text = "Local File")
                                }
                            }
                            if (!track.formatName.isNullOrBlank()) {
                                Text(
                                    text = track.formatName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (track.isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = CyanPrimary
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Online Button
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenOnlineSearch() }
                        .testTag("open_online_subtitles_button")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Online Subtitles", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Add File Button
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { filePicker.launch(arrayOf("*/*")) }
                        .testTag("open_file_subtitle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Open Sub File", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add URL Button
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenAddUrl() }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Add Sub URL", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Appearance Button
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenStyleSettings() }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Style & Size", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BadgeChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CyanPrimary.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanPrimary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedSheet(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Playback Speed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(speeds) { speed ->
                    val isSelected = speed == currentSpeed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectSpeed(speed) }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .testTag("speed_item_${speed}x"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (speed == 1.0f) "1.0x (Normal Speed)" else "${speed}x",
                            fontSize = 16.sp,
                            color = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = CyanPrimary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
