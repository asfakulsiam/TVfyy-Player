package com.example.ui.playlist.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.parser.M3uExportOptions
import com.example.domain.model.Playlist
import com.example.ui.theme.CyanPrimary

enum class ExportScope {
    ENTIRE_PLAYLIST,
    CURRENT_CATEGORY,
    SELECTED_CHANNELS
}

@Composable
fun ExportPlaylistDialog(
    playlist: Playlist,
    currentCategory: String,
    selectedChannelCount: Int,
    onDismiss: () -> Unit,
    onExportTrigger: (scope: ExportScope, options: M3uExportOptions, uri: Uri) -> Unit,
    onShareText: (scope: ExportScope, options: M3uExportOptions) -> Unit
) {
    val context = LocalContext.current
    var scope by remember {
        mutableStateOf(
            if (selectedChannelCount > 0) ExportScope.SELECTED_CHANNELS else ExportScope.ENTIRE_PLAYLIST
        )
    }

    var includeTvg by remember { mutableStateOf(true) }
    var includeLogos by remember { mutableStateOf(true) }
    var includeCategories by remember { mutableStateOf(true) }
    var includeCustomAttrs by remember { mutableStateOf(true) }

    val defaultFileName = remember(playlist.name, scope) {
        val sanitized = playlist.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        when (scope) {
            ExportScope.ENTIRE_PLAYLIST -> "${sanitized}.m3u"
            ExportScope.CURRENT_CATEGORY -> "${sanitized}_${currentCategory.replace(" ", "_")}.m3u"
            ExportScope.SELECTED_CHANNELS -> "${sanitized}_selected.m3u"
        }
    }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri: Uri? ->
        if (uri != null) {
            val options = M3uExportOptions(
                includeTvgMetadata = includeTvg,
                includeLogos = includeLogos,
                includeCategories = includeCategories,
                includeCustomAttributes = includeCustomAttrs
            )
            onExportTrigger(scope, options, uri)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.IosShare, null, tint = CyanPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export Playlist",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Export '${playlist.name}' back to a valid extended M3U file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Export Scope",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { scope = ExportScope.ENTIRE_PLAYLIST }
                ) {
                    RadioButton(
                        selected = scope == ExportScope.ENTIRE_PLAYLIST,
                        onClick = { scope = ExportScope.ENTIRE_PLAYLIST },
                        colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                    )
                    Text("Entire Playlist (${playlist.channelCount} channels)")
                }

                if (currentCategory != "All") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope = ExportScope.CURRENT_CATEGORY }
                    ) {
                        RadioButton(
                            selected = scope == ExportScope.CURRENT_CATEGORY,
                            onClick = { scope = ExportScope.CURRENT_CATEGORY },
                            colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                        )
                        Text("Current Category '$currentCategory'")
                    }
                }

                if (selectedChannelCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope = ExportScope.SELECTED_CHANNELS }
                    ) {
                        RadioButton(
                            selected = scope == ExportScope.SELECTED_CHANNELS,
                            onClick = { scope = ExportScope.SELECTED_CHANNELS },
                            colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                        )
                        Text("Selected Channels ($selectedChannelCount channels)")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Include in Output",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { includeTvg = !includeTvg }
                ) {
                    Checkbox(checked = includeTvg, onCheckedChange = { includeTvg = it }, colors = CheckboxDefaults.colors(checkedColor = CyanPrimary))
                    Text("TVG metadata (tvg-id, tvg-name)", fontSize = 13.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { includeLogos = !includeLogos }
                ) {
                    Checkbox(checked = includeLogos, onCheckedChange = { includeLogos = it }, colors = CheckboxDefaults.colors(checkedColor = CyanPrimary))
                    Text("Channel logos (tvg-logo)", fontSize = 13.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { includeCategories = !includeCategories }
                ) {
                    Checkbox(checked = includeCategories, onCheckedChange = { includeCategories = it }, colors = CheckboxDefaults.colors(checkedColor = CyanPrimary))
                    Text("Category groups (group-title)", fontSize = 13.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { includeCustomAttrs = !includeCustomAttrs }
                ) {
                    Checkbox(checked = includeCustomAttrs, onCheckedChange = { includeCustomAttrs = it }, colors = CheckboxDefaults.colors(checkedColor = CyanPrimary))
                    Text("Custom preserved attributes", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { createDocLauncher.launch(defaultFileName) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                modifier = Modifier.testTag("save_m3u_button")
            ) {
                Text("Save to Storage", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
