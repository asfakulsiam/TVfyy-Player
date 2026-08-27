package com.example.ui.playlist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ChannelSortOrder
import com.example.domain.model.Playlist
import com.example.domain.model.PlaylistCategory
import com.example.domain.model.PlaylistChannel
import com.example.domain.model.PlaylistSourceType
import com.example.ui.playlist.components.ChannelItemRow
import com.example.ui.playlist.dialogs.CategoryManagerDialog
import com.example.ui.playlist.dialogs.ChannelEditorDialog
import com.example.ui.playlist.dialogs.ExportPlaylistDialog
import com.example.ui.playlist.dialogs.ExportScope
import com.example.ui.playlist.dialogs.RefreshReviewDialog
import com.example.ui.theme.CyanPrimary
import com.example.ui.viewmodel.PlaylistViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: PlaylistViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlist = remember(playlists, playlistId) { playlists.find { it.id == playlistId } }

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val channels by viewModel.displayedChannels.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val filterFavorites by viewModel.filterFavorites.collectAsStateWithLifecycle()
    val isBulkSelectionMode by viewModel.isBulkSelectionMode.collectAsStateWithLifecycle()
    val selectedChannelIds by viewModel.selectedChannelIds.collectAsStateWithLifecycle()
    val refreshSummary by viewModel.refreshSummary.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var isSearchActive by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    // Dialog states
    var channelToEdit by remember { mutableStateOf<PlaylistChannel?>(null) }
    var isAddChannelOpen by remember { mutableStateOf(false) }
    var isManageCategoriesOpen by remember { mutableStateOf(false) }
    var isExportDialogOpen by remember { mutableStateOf(false) }
    var channelToMoveCategory by remember { mutableStateOf<PlaylistChannel?>(null) }
    var isBulkMoveCategoryOpen by remember { mutableStateOf(false) }
    var targetMoveCategoryName by remember { mutableStateOf("") }
    var channelToDelete by remember { mutableStateOf<PlaylistChannel?>(null) }

    LaunchedEffect(playlistId) {
        viewModel.selectPlaylist(playlistId)
    }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { msg ->
            if (msg.contains("deleted", ignoreCase = true)) {
                val result = snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    if (msg.contains("channels deleted", ignoreCase = true)) {
                        viewModel.undoBulkDelete()
                    } else {
                        viewModel.undoDeleteChannel()
                    }
                }
            } else {
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    Scaffold(
        topBar = {
            if (isBulkSelectionMode) {
                // Bulk Selection Top Bar
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedChannelIds.size} Selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleBulkSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAllDisplayedChannels() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(
                            onClick = { isBulkMoveCategoryOpen = true },
                            enabled = selectedChannelIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = "Move Category")
                        }
                        IconButton(
                            onClick = { isExportDialogOpen = true },
                            enabled = selectedChannelIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.IosShare, contentDescription = "Export Selected")
                        }
                        IconButton(
                            onClick = { viewModel.bulkDeleteSelectedChannels() },
                            enabled = selectedChannelIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = playlist?.name ?: "Playlist",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${channels.size} channels • ${categories.size} categories",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Search Toggle
                        IconButton(onClick = { isSearchActive = !isSearchActive }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }

                        // Sort Menu Action
                        Box {
                            IconButton(onClick = { sortMenuExpanded = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                            }

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                ChannelSortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.displayName) },
                                        leadingIcon = {
                                            if (sortOrder == order) {
                                                Icon(Icons.Default.Check, null, tint = CyanPrimary)
                                            }
                                        },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Overflow Options
                        Box {
                            IconButton(onClick = { overflowMenuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }

                            DropdownMenu(
                                expanded = overflowMenuExpanded,
                                onDismissRequest = { overflowMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Select Multiple") },
                                    leadingIcon = { Icon(Icons.Default.SelectAll, null) },
                                    onClick = {
                                        overflowMenuExpanded = false
                                        viewModel.toggleBulkSelectionMode()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Manage Categories") },
                                    leadingIcon = { Icon(Icons.Default.Folder, null) },
                                    onClick = {
                                        overflowMenuExpanded = false
                                        isManageCategoriesOpen = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export M3U") },
                                    leadingIcon = { Icon(Icons.Default.IosShare, null) },
                                    onClick = {
                                        overflowMenuExpanded = false
                                        isExportDialogOpen = true
                                    }
                                )
                                if (playlist?.sourceType == PlaylistSourceType.REMOTE_URL || playlist?.sourceType == PlaylistSourceType.GITHUB_RAW) {
                                    DropdownMenuItem(
                                        text = { Text("Refresh Playlist") },
                                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                                        onClick = {
                                            overflowMenuExpanded = false
                                            viewModel.checkPlaylistRefresh(playlistId)
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        },
        floatingActionButton = {
            if (!isBulkSelectionMode) {
                FloatingActionButton(
                    onClick = { isAddChannelOpen = true },
                    containerColor = CyanPrimary,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.testTag("add_channel_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Channel")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Expandable Search Bar
            AnimatedVisibility(visible = isSearchActive) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Search channel, TVG name, category...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = CyanPrimary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, null)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("channel_search_bar")
                    )
                }
            }

            // Categories Filter Chip Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Favorites Chip
                FilterChip(
                    selected = filterFavorites,
                    onClick = { viewModel.toggleFilterFavorites() },
                    label = { Text("Favorites") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (filterFavorites) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (filterFavorites) Color(0xFFFFB800) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFB800).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFFFB800)
                    )
                )

                // "All" Category Chip
                FilterChip(
                    selected = selectedCategory == "All",
                    onClick = { viewModel.selectCategory("All") },
                    label = { Text("All (${playlists.find { it.id == playlistId }?.channelCount ?: 0})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary,
                        selectedLabelColor = Color.Black
                    )
                )

                // Specific Category Chips
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory.equals(cat.name, ignoreCase = true),
                        onClick = { viewModel.selectCategory(cat.name) },
                        label = { Text("${cat.name} (${cat.channelCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }

                // Manage Categories Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { isManageCategoriesOpen = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Folder, null, tint = CyanPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manage", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyanPrimary)
                    }
                }
            }

            // Channel LazyColumn
            if (channels.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No channels found for '$searchQuery'" else "No channels in this category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { isAddChannelOpen = true }) {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Channel")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(channels, key = { _, it -> it.id }) { index, channel ->
                        ChannelItemRow(
                            channel = channel,
                            isSelectionMode = isBulkSelectionMode,
                            isSelected = selectedChannelIds.contains(channel.id),
                            onSelectToggle = { viewModel.toggleChannelSelection(channel.id) },
                            onClick = {
                                if (isBulkSelectionMode) {
                                    viewModel.toggleChannelSelection(channel.id)
                                } else {
                                    viewModel.playChannel(channel)
                                }
                            },
                            onPlay = { viewModel.playChannel(channel) },
                            onEdit = { channelToEdit = channel },
                            onDuplicate = { viewModel.duplicateChannel(channel.id) },
                            onMoveCategory = {
                                channelToMoveCategory = channel
                                targetMoveCategoryName = categories.firstOrNull()?.name ?: "Uncategorized"
                            },
                            onToggleFavorite = { viewModel.toggleChannelFavorite(channel) },
                            onCopyUrl = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Stream URL", channel.streamUrl))
                                scope.launch { snackbarHostState.showSnackbar("Stream URL copied to clipboard") }
                            },
                            onDelete = { channelToDelete = channel }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Channel Dialog
    if (isAddChannelOpen || channelToEdit != null) {
        ChannelEditorDialog(
            channel = channelToEdit,
            playlistId = playlistId,
            categories = categories,
            onDismiss = {
                isAddChannelOpen = false
                channelToEdit = null
            },
            onSave = { savedChannel ->
                viewModel.saveChannel(savedChannel)
                isAddChannelOpen = false
                channelToEdit = null
            }
        )
    }

    // Delete Channel Confirmation Dialog
    if (channelToDelete != null) {
        val channel = channelToDelete!!
        AlertDialog(
            onDismissRequest = { channelToDelete = null },
            title = { Text("Delete Channel?") },
            text = { Text("Are you sure you want to delete '${channel.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChannel(channel)
                        channelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { channelToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Category Manager Dialog
    if (isManageCategoriesOpen) {
        CategoryManagerDialog(
            categories = categories,
            onDismiss = { isManageCategoriesOpen = false },
            onAddCategory = { newName -> viewModel.addCategory(playlistId, newName) },
            onRenameCategory = { cat, newName -> viewModel.renameCategory(playlistId, cat.id, newName) },
            onDeleteCategory = { cat, deleteChannels -> viewModel.deleteCategory(playlistId, cat.id, deleteChannels) },
            onMergeCategories = { fromCat, toCat -> viewModel.mergeCategories(playlistId, fromCat, toCat) },
            onMovePosition = { from, to -> viewModel.moveCategoryPosition(playlistId, from, to) }
        )
    }

    // Move Channel Category Dialog
    if (channelToMoveCategory != null) {
        val ch = channelToMoveCategory!!
        AlertDialog(
            onDismissRequest = { channelToMoveCategory = null },
            title = { Text("Move '${ch.name}' to Category") },
            text = {
                Column {
                    categories.forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { targetMoveCategoryName = cat.name }
                                .padding(vertical = 6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (targetMoveCategoryName == cat.name) CyanPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = cat.name,
                                    fontWeight = if (targetMoveCategoryName == cat.name) FontWeight.Bold else FontWeight.Normal,
                                    color = if (targetMoveCategoryName == cat.name) CyanPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val matched = categories.find { it.name == targetMoveCategoryName }
                        viewModel.moveChannelCategory(ch.id, matched?.id, targetMoveCategoryName)
                        channelToMoveCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Move", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { channelToMoveCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bulk Move Category Dialog
    if (isBulkMoveCategoryOpen) {
        AlertDialog(
            onDismissRequest = { isBulkMoveCategoryOpen = false },
            title = { Text("Move ${selectedChannelIds.size} Channels to Category") },
            text = {
                Column {
                    categories.forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { targetMoveCategoryName = cat.name }
                                .padding(vertical = 6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (targetMoveCategoryName == cat.name) CyanPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = cat.name,
                                    fontWeight = if (targetMoveCategoryName == cat.name) FontWeight.Bold else FontWeight.Normal,
                                    color = if (targetMoveCategoryName == cat.name) CyanPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val matched = categories.find { it.name == targetMoveCategoryName }
                        viewModel.bulkMoveCategory(matched?.id, targetMoveCategoryName)
                        isBulkMoveCategoryOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Move Selected", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { isBulkMoveCategoryOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Refresh Change Review Dialog
    if (refreshSummary != null) {
        RefreshReviewDialog(
            summary = refreshSummary!!,
            onDismiss = { viewModel.dismissRefreshSummary() },
            onConfirmUpdate = { keepLocalEdits ->
                viewModel.applyRefreshUpdate(keepLocalEdits)
            }
        )
    }

    // Export Playlist Dialog
    if (isExportDialogOpen && playlist != null) {
        ExportPlaylistDialog(
            playlist = playlist,
            currentCategory = selectedCategory,
            selectedChannelCount = selectedChannelIds.size,
            onDismiss = { isExportDialogOpen = false },
            onExportTrigger = { scopeOption, options, uri ->
                try {
                    val categoryFilter = if (scopeOption == ExportScope.CURRENT_CATEGORY) selectedCategory else null
                    val selectedIds = if (scopeOption == ExportScope.SELECTED_CHANNELS) selectedChannelIds.toList() else null

                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        viewModel.exportPlaylistToStream(
                            outputStream = os,
                            playlistId = playlist.id,
                            options = options,
                            categoryFilter = categoryFilter,
                            selectedChannelIds = selectedIds,
                            onComplete = { isExportDialogOpen = false }
                        )
                    }
                } catch (_: Exception) {}
            },
            onShareText = { _, _ -> }
        )
    }
}
