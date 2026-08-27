package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.BufferPreset
import com.example.domain.model.SubtitleAutoSelectMode
import com.example.subtitles.SubtitleManager
import com.example.ui.theme.CyanPrimary

import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import com.example.BuildConfig
import com.example.domain.model.UpdateCheckState
import com.example.ui.components.TvFyyLogo

@Composable
fun SettingsScreen(
    onNavigateToProfiles: () -> Unit,
    onNavigateToSupport: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    updateState: UpdateCheckState? = null,
    autoCheckUpdates: Boolean = true,
    onToggleAutoCheckUpdates: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val subtitleManager = remember { SubtitleManager(context) }

    var hardwareAcceleration by remember { mutableStateOf(true) }
    var autoPipEnabled by remember { mutableStateOf(true) }
    var gestureControlsEnabled by remember { mutableStateOf(true) }
    var keepScreenAwake by remember { mutableStateOf(true) }
    var doubleTapSeekSeconds by remember { mutableIntStateOf(10) }
    var selectedBufferPreset by remember { mutableStateOf(BufferPreset.BALANCED) }

    var preferredAudioLang by remember { mutableStateOf("en") }
    var preferredSubLang by remember { mutableStateOf("en") }
    var subtitleAutoSelect by remember { mutableStateOf(SubtitleAutoSelectMode.AUTO_MATCH) }
    var cacheStats by remember { mutableStateOf(subtitleManager.getCacheSizeFormatted()) }

    val languages = listOf(
        "bn" to "বাংলা (Bangla)",
        "en" to "English",
        "hi" to "हिन्दी (Hindi)",
        "es" to "Español",
        "ar" to "العربية (Arabic)",
        "ja" to "日本語 (Japanese)",
        "fr" to "Français",
        "de" to "Deutsch"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Player Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Configure engine decoders, network buffers, audio & subtitle preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
            }
        }

        // Subtitles & Multi-Audio System Category
        item {
            SettingsCategoryHeader(title = "Subtitles & Multi-Audio System")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Preferred Audio Language
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Preferred Audio Language",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languages.forEach { (code, name) ->
                            val isSelected = preferredAudioLang == code
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CyanPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { preferredAudioLang = code }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color(0xFF00363D) else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                    // Preferred Subtitle Language
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Subtitles, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Preferred Subtitle Language",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languages.forEach { (code, name) ->
                            val isSelected = preferredSubLang == code
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CyanPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { preferredSubLang = code }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color(0xFF00363D) else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                    // Subtitle Auto-Select
                    Text(
                        text = "Subtitle Auto-Select Policy",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SubtitleAutoSelectMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { subtitleAutoSelect = mode }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = subtitleAutoSelect == mode,
                                onClick = { subtitleAutoSelect = mode },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = mode.displayName,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                    // Subtitle Cache Management
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Downloaded Subtitles Cache",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Current cache size: $cacheStats",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                subtitleManager.clearSubtitleCache()
                                cacheStats = subtitleManager.getCacheSizeFormatted()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Cache", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Engine & Decoder Settings
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SettingsCategoryHeader(title = "Engine & Decoders")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingToggleRow(
                        title = "Hardware Acceleration",
                        subtitle = "Prefer device GPU hardware decoders for 4K / 60fps streams",
                        icon = Icons.Default.Memory,
                        checked = hardwareAcceleration,
                        onCheckedChange = { hardwareAcceleration = it }
                    )
                    HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                    SettingToggleRow(
                        title = "Keep Screen On",
                        subtitle = "Prevent display from sleeping during active playback",
                        icon = Icons.Default.DisplaySettings,
                        checked = keepScreenAwake,
                        onCheckedChange = { keepScreenAwake = it }
                    )
                }
            }
        }

        // Buffer Presets
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SettingsCategoryHeader(title = "Network & Stream Buffering")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BufferPreset.values().forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedBufferPreset = preset }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedBufferPreset == preset,
                                onClick = { selectedBufferPreset = preset },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = preset.displayName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = preset.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Gesture & Controls
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SettingsCategoryHeader(title = "Gestures & Seeking")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingToggleRow(
                        title = "Touch Gestures",
                        subtitle = "Swipe for brightness, volume, horizontal seek & pinch-to-zoom",
                        icon = Icons.Default.TouchApp,
                        checked = gestureControlsEnabled,
                        onCheckedChange = { gestureControlsEnabled = it }
                    )
                    HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                    SettingToggleRow(
                        title = "Auto Picture-in-Picture",
                        subtitle = "Enter PiP automatically when exiting player",
                        icon = Icons.Default.PictureInPicture,
                        checked = autoPipEnabled,
                        onCheckedChange = { autoPipEnabled = it }
                    )
                    HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "Double-Tap Seek Step",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 30).forEach { seconds ->
                            val isSelected = doubleTapSeekSeconds == seconds
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CyanPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { doubleTapSeekSeconds = seconds }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${seconds}s",
                                    color = if (isSelected) Color(0xFF00363D) else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Header Profiles Quick Jump
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SettingsCategoryHeader(title = "Network & Security")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToProfiles() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom Header Profiles",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Manage User-Agent, Referer, and Bearer Tokens for streams",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Support Developer / Buy Me a Coffee
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SettingsCategoryHeader(title = "Support & Contribution")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToSupport() }
                    .testTag("settings_buy_coffee_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "☕", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Buy Me a Coffee",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Support independent TVfyy Player development",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CyanPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Support",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // GitHub Releases & Auto-Updater Section
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SettingsCategoryHeader(title = "App Updates")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_updates_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Auto-Check Toggle
                    SettingToggleRow(
                        title = "Auto-Check on Startup",
                        subtitle = "Automatically check for new releases when opening the app",
                        icon = Icons.Default.SystemUpdate,
                        checked = autoCheckUpdates,
                        onCheckedChange = onToggleAutoCheckUpdates
                    )

                    HorizontalDivider(
                        color = DividerDefaults.color.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Manual Check Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Installed Version: v${BuildConfig.VERSION_NAME}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (updateState?.errorMessage != null) {
                                Text(
                                    text = updateState.errorMessage,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF8080)
                                )
                            } else if (updateState?.updateInfo != null && !updateState.updateInfo.isUpdateAvailable) {
                                Text(
                                    text = "You are using the latest version",
                                    fontSize = 11.sp,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        Button(
                            onClick = onCheckForUpdates,
                            enabled = updateState?.isChecking != true,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("settings_check_update_button")
                        ) {
                            if (updateState?.isChecking == true) {
                                CircularProgressIndicator(
                                    color = Color(0xFF00363D),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Checking...",
                                    color = Color(0xFF00363D),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color(0xFF00363D),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Check Now",
                                    color = Color(0xFF00363D),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // About & Version Card
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SettingsCategoryHeader(title = "About TVfyy")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TvFyyLogo(
                        size = 72.dp,
                        showText = true,
                        animateGlow = false
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "High-Performance Stream & Media Player for Android. Built with native Media3 ExoPlayer engine, HLS / DASH adaptive streaming, custom URL profile authentication, smart subtitles, and automated GitHub releases.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = CyanPrimary,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00363D),
                checkedTrackColor = CyanPrimary
            )
        )
    }
}
