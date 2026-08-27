package com.example.ui.playlist

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.parser.M3uExportOptions
import com.example.domain.model.Playlist
import com.example.ui.playlist.components.PlaylistCard
import com.example.ui.playlist.dialogs.AddPlaylistBottomSheet
import com.example.ui.playlist.dialogs.ExportPlaylistDialog
import com.example.ui.playlist.dialogs.ExportScope
import com.example.ui.playlist.dialogs.RefreshReviewDialog
import com.example.ui.theme.CyanPrimary
import com.example.ui.viewmodel.PlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    viewModel: PlaylistViewModel,
    onOpenPlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val refreshSummary by viewModel.refreshSummary.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var isAddSheetOpen by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var playlistToExport by remember { mutableStateOf<Playlist?>(null) }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlaylistPlay,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Playlists",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddSheetOpen = true },
                containerColor = CyanPrimary,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.testTag("add_playlist_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Playlist")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (playlists.isEmpty()) {
                // Empty State
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .background(CyanPrimary.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No Playlists Yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Import M3U/M3U8 playlists from local storage, remote URLs, GitHub raw links, or paste text directly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { isAddSheetOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.Black)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Add Playlist", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = {
                                viewModel.selectPlaylist(playlist.id)
                                onOpenPlaylist(playlist.id)
                            },
                            onRename = {
                                playlistToRename = playlist
                                renameInput = playlist.name
                            },
                            onRefresh = {
                                viewModel.checkPlaylistRefresh(playlist.id)
                            },
                            onExport = {
                                playlistToExport = playlist
                            },
                            onDuplicate = {
                                viewModel.duplicatePlaylist(playlist.id)
                            },
                            onDelete = {
                                playlistToDelete = playlist
                            },
                            onSetActive = {
                                viewModel.setActivePlaylist(playlist.id)
                            }
                        )
                    }
                }
            }

            if (isRefreshing) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            CircularProgressIndicator(color = CyanPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.size(16.dp))
                            Text("Checking remote playlist updates...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    // Add / Import Bottom Sheet
    if (isAddSheetOpen) {
        AddPlaylistBottomSheet(
            viewModel = viewModel,
            importState = importState,
            onDismiss = {
                isAddSheetOpen = false
                viewModel.resetImportState()
            },
            onSuccess = { newId ->
                isAddSheetOpen = false
                viewModel.selectPlaylist(newId)
                onOpenPlaylist(newId)
            }
        )
    }

    // Rename Dialog
    if (playlistToRename != null) {
        val playlist = playlistToRename!!
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            title = { Text("Rename Playlist") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renamePlaylist(playlist.id, renameInput.trim())
                            playlistToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Rename", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (playlistToDelete != null) {
        val playlist = playlistToDelete!!
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("Delete Playlist?") },
            text = {
                Text("Are you sure you want to delete '${playlist.name}'? This will remove all ${playlist.channelCount} channels and ${playlist.categoryCount} categories.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlaylist(playlist)
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
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
    if (playlistToExport != null) {
        val playlist = playlistToExport!!
        ExportPlaylistDialog(
            playlist = playlist,
            currentCategory = "All",
            selectedChannelCount = 0,
            onDismiss = { playlistToExport = null },
            onExportTrigger = { scope, options, uri ->
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        viewModel.exportPlaylistToStream(
                            outputStream = os,
                            playlistId = playlist.id,
                            options = options,
                            onComplete = { playlistToExport = null }
                        )
                    }
                } catch (_: Exception) {}
            },
            onShareText = { _, _ -> }
        )
    }
}
