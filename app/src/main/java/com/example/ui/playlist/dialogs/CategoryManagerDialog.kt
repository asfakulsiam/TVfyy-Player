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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PlaylistCategory
import com.example.ui.theme.CyanPrimary

@Composable
fun CategoryManagerDialog(
    categories: List<PlaylistCategory>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (PlaylistCategory, String) -> Unit,
    onDeleteCategory: (PlaylistCategory, Boolean) -> Unit,
    onMergeCategories: (fromCategory: String, toCategory: String) -> Unit,
    onMovePosition: (fromIndex: Int, toIndex: Int) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<PlaylistCategory?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var mergingCategory by remember { mutableStateOf<PlaylistCategory?>(null) }
    var targetMergeCategoryName by remember { mutableStateOf("") }

    var deletingCategory by remember { mutableStateOf<PlaylistCategory?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, null, tint = CyanPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Manage Categories",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                // Add New Category Input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("New Category Name") },
                        placeholder = { Text("e.g. Cinema") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_category_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAddCategory(newCategoryName.trim())
                                newCategoryName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Categories
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(categories, key = { _, it -> it.id }) { index, category ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${category.channelCount} channels",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Reorder buttons
                                IconButton(
                                    onClick = { if (index > 0) onMovePosition(index, index - 1) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, "Move Up", modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { if (index < categories.size - 1) onMovePosition(index, index + 1) },
                                    enabled = index < categories.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, "Move Down", modifier = Modifier.size(16.dp))
                                }

                                // Edit / Rename
                                IconButton(
                                    onClick = {
                                        editingCategory = category
                                        renameInput = category.name
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, "Rename", tint = CyanPrimary, modifier = Modifier.size(16.dp))
                                }

                                // Merge
                                IconButton(
                                    onClick = {
                                        mergingCategory = category
                                        targetMergeCategoryName = categories.firstOrNull { it.id != category.id }?.name ?: ""
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.CallMerge, "Merge", tint = Color(0xFFFFB800), modifier = Modifier.size(16.dp))
                                }

                                // Delete
                                IconButton(
                                    onClick = { deletingCategory = category },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )

    // Rename Sub-dialog
    if (editingCategory != null) {
        val cat = editingCategory!!
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Rename Category") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRenameCategory(cat, renameInput.trim())
                            editingCategory = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Rename", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Merge Sub-dialog
    if (mergingCategory != null) {
        val fromCat = mergingCategory!!
        val otherCategories = categories.filter { it.id != fromCat.id }

        AlertDialog(
            onDismissRequest = { mergingCategory = null },
            title = { Text("Merge Category '${fromCat.name}'") },
            text = {
                Column {
                    Text(
                        text = "Move all ${fromCat.channelCount} channels from '${fromCat.name}' into:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    otherCategories.forEach { targetCat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { targetMergeCategoryName = targetCat.name }
                                .padding(vertical = 6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (targetMergeCategoryName == targetCat.name) CyanPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        text = "${targetCat.name} (${targetCat.channelCount} channels)",
                                        fontWeight = if (targetMergeCategoryName == targetCat.name) FontWeight.Bold else FontWeight.Normal,
                                        color = if (targetMergeCategoryName == targetCat.name) CyanPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetMergeCategoryName.isNotBlank()) {
                            onMergeCategories(fromCat.name, targetMergeCategoryName)
                            mergingCategory = null
                        }
                    },
                    enabled = targetMergeCategoryName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Merge", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { mergingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Sub-dialog
    if (deletingCategory != null) {
        val cat = deletingCategory!!
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Delete Category '${cat.name}'?") },
            text = {
                Text(
                    text = "This category contains ${cat.channelCount} channels. Do you want to keep the channels by moving them to 'Uncategorized', or delete all channels inside?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat, false) // Move channels to Uncategorized
                        deletingCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Keep Channels", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(cat, true) // Delete channels
                        deletingCategory = null
                    }
                ) {
                    Text("Delete Everything", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}
