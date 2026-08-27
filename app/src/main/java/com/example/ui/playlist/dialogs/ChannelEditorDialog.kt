package com.example.ui.playlist.dialogs

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.PlaylistCategory
import com.example.domain.model.PlaylistChannel
import com.example.ui.theme.CyanPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelEditorDialog(
    channel: PlaylistChannel?,
    playlistId: Long,
    categories: List<PlaylistCategory>,
    onDismiss: () -> Unit,
    onSave: (PlaylistChannel) -> Unit
) {
    val isEditing = channel != null

    var name by remember { mutableStateOf(channel?.name ?: "") }
    var streamUrl by remember { mutableStateOf(channel?.streamUrl ?: "") }
    var categoryName by remember { mutableStateOf(channel?.categoryName ?: (categories.firstOrNull()?.name ?: "Uncategorized")) }
    var tvgId by remember { mutableStateOf(channel?.tvgId ?: "") }
    var tvgName by remember { mutableStateOf(channel?.tvgName ?: "") }
    var tvgLogo by remember { mutableStateOf(channel?.tvgLogo ?: "") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showAdvancedAttributes by remember { mutableStateOf(false) }
    var customAttributes by remember {
        mutableStateOf(
            (channel?.unknownAttributes ?: emptyMap()).toMutableMap()
        )
    }

    var newAttrKey by remember { mutableStateOf("") }
    var newAttrVal by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Channel" else "Add Channel",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Channel Name (Required)
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Channel Name *") },
                    placeholder = { Text("e.g. Unite8 Sports 1") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("channel_name_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Stream URL (Required)
                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = {
                        streamUrl = it
                        errorMessage = null
                    },
                    label = { Text("Stream URL *") },
                    placeholder = { Text("https://example.com/stream.m3u8") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("channel_stream_url_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    categoryName = cat.name
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Logo URL & Preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = tvgLogo,
                        onValueChange = { tvgLogo = it },
                        label = { Text("Logo URL (tvg-logo)") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        if (tvgLogo.isNotBlank()) {
                            AsyncImage(
                                model = tvgLogo,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(40.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TVG ID
                OutlinedTextField(
                    value = tvgId,
                    onValueChange = { tvgId = it },
                    label = { Text("TVG ID (tvg-id)") },
                    placeholder = { Text("f27de012-...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // TVG Name
                OutlinedTextField(
                    value = tvgName,
                    onValueChange = { tvgName = it },
                    label = { Text("TVG Name (tvg-name)") },
                    placeholder = { Text("Unite8 Sports 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle Advanced Attributes
                TextButton(
                    onClick = { showAdvancedAttributes = !showAdvancedAttributes },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(
                        text = if (showAdvancedAttributes) "Hide Advanced Attributes" else "Show Advanced Attributes (${customAttributes.size})",
                        color = CyanPrimary,
                        fontSize = 13.sp
                    )
                }

                if (showAdvancedAttributes) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        customAttributes.forEach { (k, v) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$k=\"$v\"",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val updated = customAttributes.toMutableMap()
                                        updated.remove(k)
                                        customAttributes = updated
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newAttrKey,
                                onValueChange = { newAttrKey = it },
                                label = { Text("Key", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            OutlinedTextField(
                                value = newAttrVal,
                                onValueChange = { newAttrVal = it },
                                label = { Text("Value", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    if (newAttrKey.isNotBlank()) {
                                        val updated = customAttributes.toMutableMap()
                                        updated[newAttrKey.trim()] = newAttrVal.trim()
                                        customAttributes = updated
                                        newAttrKey = ""
                                        newAttrVal = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, "Add attribute", tint = CyanPrimary)
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val cleanUrl = streamUrl.trim()
                    if (cleanName.isBlank()) {
                        errorMessage = "Channel name is required"
                        return@Button
                    }
                    if (cleanUrl.isBlank()) {
                        errorMessage = "Stream URL is required"
                        return@Button
                    }

                    val matchedCategory = categories.find { it.name.equals(categoryName.trim(), ignoreCase = true) }

                    val savedChannel = (channel ?: PlaylistChannel(
                        playlistId = playlistId,
                        name = cleanName,
                        streamUrl = cleanUrl
                    )).copy(
                        name = cleanName,
                        streamUrl = cleanUrl,
                        categoryId = matchedCategory?.id,
                        categoryName = categoryName.trim().ifBlank { "Uncategorized" },
                        tvgId = tvgId.trim().ifBlank { null },
                        tvgName = tvgName.trim().ifBlank { null },
                        tvgLogo = tvgLogo.trim().ifBlank { null },
                        unknownAttributes = customAttributes,
                        isUserEdited = true
                    )

                    onSave(savedChannel)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                modifier = Modifier.testTag("save_channel_button")
            ) {
                Text(if (isEditing) "Save Changes" else "Add Channel", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
