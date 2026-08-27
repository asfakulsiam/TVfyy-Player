package com.example.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.StreamDiagnostics
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.CyanPrimary
import java.util.Locale

@Composable
fun StreamInfoDialog(
    diagnostics: StreamDiagnostics,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Stream & Decoder Info",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reachability Badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (diagnostics.reachable) AccentSuccess.copy(alpha = 0.15f) else AccentError.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (diagnostics.reachable) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (diagnostics.reachable) AccentSuccess else AccentError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (diagnostics.reachable) "Stream Reachable (${diagnostics.httpStatusCode ?: 200})" else "Unreachable / Offline",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (diagnostics.reachable) AccentSuccess else AccentError
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info Rows
                InfoItem(label = "Stream Type", value = diagnostics.detectedStreamType.displayName)
                if (diagnostics.maskedUrl.isNotBlank()) {
                    InfoItem(label = "Stream URL", value = diagnostics.maskedUrl)
                }
                if (!diagnostics.contentType.isNullOrBlank()) {
                    InfoItem(label = "Content-Type", value = diagnostics.contentType)
                }
                if (diagnostics.resolution != null) {
                    InfoItem(label = "Resolution", value = diagnostics.resolution)
                }
                if (!diagnostics.videoCodec.isNullOrBlank()) {
                    InfoItem(label = "Video Codec", value = diagnostics.videoCodec)
                }
                if (!diagnostics.videoDecoderName.isNullOrBlank()) {
                    val hwBadge = if (diagnostics.isHardwareVideoDecoder == true) " (Hardware GPU)" else " (Software CPU)"
                    InfoItem(label = "Video Decoder", value = "${diagnostics.videoDecoderName}$hwBadge")
                }
                if (!diagnostics.audioCodec.isNullOrBlank()) {
                    InfoItem(label = "Audio Codec", value = diagnostics.audioCodec)
                }
                if (!diagnostics.audioDecoderName.isNullOrBlank()) {
                    val hwBadge = if (diagnostics.isHardwareAudioDecoder == true) " (Hardware)" else " (Software)"
                    InfoItem(label = "Audio Decoder", value = "${diagnostics.audioDecoderName}$hwBadge")
                }
                if (diagnostics.bitrate != null && diagnostics.bitrate > 0) {
                    val bitrateFormatted = String.format(Locale.US, "%.2f Mbps", diagnostics.bitrate / 1_000_000f)
                    InfoItem(label = "Bitrate", value = bitrateFormatted)
                }
                if (diagnostics.frameRate != null && diagnostics.frameRate > 0) {
                    InfoItem(label = "Frame Rate", value = "${diagnostics.frameRate.toInt()} FPS")
                }
                if (diagnostics.bufferedAheadMs != null && diagnostics.bufferedAheadMs > 0) {
                    InfoItem(label = "Buffer Ahead", value = "${diagnostics.bufferedAheadMs / 1000}s")
                }
                if (diagnostics.durationMs != null && diagnostics.durationMs > 0) {
                    InfoItem(label = "Duration", value = TimeFormatter.formatMs(diagnostics.durationMs))
                }
                if (diagnostics.videoTracksCount > 0) {
                    InfoItem(label = "Video Tracks", value = "${diagnostics.videoTracksCount} track(s)")
                }
                if (diagnostics.audioTracksCount > 0) {
                    InfoItem(label = "Audio Tracks", value = "${diagnostics.audioTracksCount} track(s)")
                }
                if (diagnostics.subtitleTracksCount > 0) {
                    InfoItem(label = "Subtitle Tracks", value = "${diagnostics.subtitleTracksCount} track(s)")
                }

                if (!diagnostics.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Diagnostics note: ${diagnostics.errorMessage}",
                        color = AccentError,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
        }
        HorizontalDivider(
            color = DividerDefaults.color.copy(alpha = 0.15f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
