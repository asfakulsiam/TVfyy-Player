package com.example.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.EventServer
import com.example.domain.model.PlaylistChannel
import com.example.domain.model.StreamType
import com.example.domain.model.TopEvent
import com.example.domain.model.UpdateInfo
import com.example.ui.home.components.EventServerSheet
import com.example.ui.home.components.SyncPlaylistDialog
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentWarning
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.PurpleTertiary
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PlaylistViewModel

@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    playlistViewModel: PlaylistViewModel,
    onNavigateToUrl: () -> Unit,
    onNavigateToPlaylists: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    availableUpdate: UpdateInfo? = null,
    modifier: Modifier = Modifier
) {
    val topEvents by playlistViewModel.topEvents.collectAsStateWithLifecycle()
    val isTopEventsLoading by playlistViewModel.isTopEventsLoading.collectAsStateWithLifecycle()
    val isDefaultUpdateAvailable by playlistViewModel.isDefaultUpdateAvailable.collectAsStateWithLifecycle()
    val showSyncConfirmModal by playlistViewModel.showSyncConfirmModal.collectAsStateWithLifecycle()
    val isSyncing by playlistViewModel.isRefreshing.collectAsStateWithLifecycle()

    val channels by playlistViewModel.channels.collectAsStateWithLifecycle()
    val categories by playlistViewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by playlistViewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by playlistViewModel.searchQuery.collectAsStateWithLifecycle()

    var selectedEventForSheet by remember { mutableStateOf<TopEvent?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { mainViewModel.onLocalMediaSelected(it) }
    }

    // Sync Confirmation Reminder Dialog
    if (showSyncConfirmModal) {
        SyncPlaylistDialog(
            onConfirm = { playlistViewModel.confirmSyncDefaultPlaylist() },
            onDismiss = { playlistViewModel.dismissSyncConfirmModal() }
        )
    }

    // Top Event Server Selection Sheet
    selectedEventForSheet?.let { event ->
        EventServerSheet(
            event = event,
            onSelectServer = { server ->
                selectedEventForSheet = null
                mainViewModel.playStream(
                    url = server.url,
                    title = "${event.title} (${server.name})",
                    streamType = StreamType.HLS
                )
            },
            onDismiss = { selectedEventForSheet = null }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // App Header with TVfyy Neon Branding & Quick Actions
        item {
            HomeTopHeader(
                isUpdateAvailable = isDefaultUpdateAvailable,
                isSyncing = isSyncing,
                onSyncClick = { playlistViewModel.openSyncConfirmModal() },
                onSearchToggle = { isSearchExpanded = !isSearchExpanded },
                channelCount = channels.size
            )
        }

        // Available App Update Alert Banner
        if (availableUpdate != null && availableUpdate.isUpdateAvailable) {
            item {
                AppUpdateBanner(
                    updateInfo = availableUpdate,
                    onClick = onCheckForUpdates
                )
            }
        }

        // Search Bar (expandable)
        if (isSearchExpanded) {
            item {
                HomeSearchBar(
                    query = searchQuery,
                    onQueryChange = { playlistViewModel.setSearchQuery(it) },
                    onClose = {
                        playlistViewModel.setSearchQuery("")
                        isSearchExpanded = false
                    }
                )
            }
        }

        // Top Events Featured Section
        if (topEvents.isNotEmpty() || isTopEventsLoading) {
            item {
                FeaturedEventsSection(
                    events = topEvents,
                    isLoading = isTopEventsLoading,
                    onEventClick = { event ->
                        if (event.servers.size == 1) {
                            mainViewModel.playStream(
                                url = event.servers.first().url,
                                title = "${event.title} (${event.servers.first().name})",
                                streamType = StreamType.HLS
                            )
                        } else {
                            selectedEventForSheet = event
                        }
                    },
                    onRefresh = { playlistViewModel.refreshTopEvents() }
                )
            }
        }

        // Bento Quick Actions Grid
        item {
            BentoQuickActions(
                onOpenFile = { filePicker.launch(arrayOf("video/*", "*/*")) },
                onOpenUrl = onNavigateToUrl,
                onOpenPlaylists = onNavigateToPlaylists,
                onOpenFavorites = onNavigateToFavorites
            )
        }

        // Live Channels Category Bar
        item {
            LiveChannelsCategoryBar(
                categories = categories.map { it.name },
                selectedCategory = selectedCategory,
                onSelectCategory = { playlistViewModel.selectCategory(it) },
                channelCount = channels.size
            )
        }

        // Live Channels List
        if (channels.isEmpty()) {
            item {
                EmptyChannelsState(
                    onSync = { playlistViewModel.openSyncConfirmModal() }
                )
            }
        } else {
            items(
                items = channels,
                key = { it.id }
            ) { channel ->
                HomeChannelItem(
                    channel = channel,
                    onPlay = { playlistViewModel.playChannel(channel) },
                    onToggleFavorite = { playlistViewModel.toggleFavorite(channel.id) }
                )
            }
        }
    }
}

@Composable
private fun HomeTopHeader(
    isUpdateAvailable: Boolean,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onSearchToggle: () -> Unit,
    channelCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Logo & Subtitle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(CyanPrimary, IndigoSecondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = Color(0xFF00363D),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TVfyy",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CyanPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "PRO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = if (channelCount > 0) "$channelCount Channels Online" else "Live IPTV Streamer",
                    fontSize = 12.sp,
                    color = Color(0xFFBAC9CC)
                )
            }
        }

        // Header Actions: Search & Sync
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onSearchToggle,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .testTag("home_search_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Channels",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box {
                IconButton(
                    onClick = onSyncClick,
                    enabled = !isSyncing,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isUpdateAvailable) CyanPrimary.copy(alpha = 0.2f) else DarkSurfaceVariant)
                        .testTag("home_sync_github_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = CyanPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync GitHub Playlist",
                            tint = if (isUpdateAvailable) CyanPrimary else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isUpdateAvailable && !isSyncing) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AccentWarning)
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppUpdateBanner(
    updateInfo: UpdateInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("home_app_update_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1838)),
        border = BorderStroke(1.dp, PurpleTertiary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PurpleTertiary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NewReleases,
                        contentDescription = null,
                        tint = PurpleTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "New App Version Available",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PurpleTertiary.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "v${updateInfo.latestVersion}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Tap to download and install update directly",
                        fontSize = 11.sp,
                        color = Color(0xFFBAC9CC)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = PurpleTertiary
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search 200+ live streams & events...", color = Color(0xFF8E9B9E), fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                } else {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Clear, contentDescription = "Close search", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceVariant,
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_search_input")
        )
    }
}

@Composable
private fun FeaturedEventsSection(
    events: List<TopEvent>,
    isLoading: Boolean,
    onEventClick: (TopEvent) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentError)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TOP LIVE EVENTS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    color = CyanPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Text(
                    text = "${events.size} Active",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = CyanPrimary
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(events, key = { it.id }) { event ->
                FeaturedEventCard(
                    event = event,
                    onClick = { onEventClick(event) }
                )
            }
        }
    }
}

@Composable
private fun FeaturedEventCard(
    event: TopEvent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("featured_event_card_${event.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (event.image.isNotBlank()) {
                AsyncImage(
                    model = event.image,
                    contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Dark gradient overlay for readable text
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Live Pill & Multi-server indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentError
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = "${event.servers.size} Servers",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFBAC9CC),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Bottom Content
                Column {
                    Text(
                        text = event.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (event.description.isNotBlank()) {
                        Text(
                            text = event.description,
                            fontSize = 11.sp,
                            color = Color(0xFFBAC9CC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Watch Stream",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                        Surface(
                            shape = CircleShape,
                            color = CyanPrimary
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color(0xFF00363D),
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoQuickActions(
    onOpenFile: () -> Unit,
    onOpenUrl: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "QUICK ACTIONS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8E9B9E),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoCard(
                title = "Open Local File",
                subtitle = "Video / M3U",
                icon = Icons.Default.FolderOpen,
                accentColor = CyanPrimary,
                onClick = onOpenFile,
                modifier = Modifier.weight(1f),
                tag = "bento_open_file"
            )

            BentoCard(
                title = "Direct URL",
                subtitle = "HLS / MP4 Stream",
                icon = Icons.Default.Link,
                accentColor = IndigoSecondary,
                onClick = onOpenUrl,
                modifier = Modifier.weight(1f),
                tag = "bento_stream_url"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoCard(
                title = "Playlists Hub",
                subtitle = "Manage & Import",
                icon = Icons.Default.PlaylistPlay,
                accentColor = PurpleTertiary,
                onClick = onOpenPlaylists,
                modifier = Modifier.weight(1f),
                tag = "bento_playlists"
            )

            BentoCard(
                title = "Favorites",
                subtitle = "Quick Access",
                icon = Icons.Default.Favorite,
                accentColor = Color(0xFFF43F5E),
                onClick = onOpenFavorites,
                modifier = Modifier.weight(1f),
                tag = "bento_favorites"
            )
        }
    }
}

@Composable
private fun BentoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFFBAC9CC)
                )
            }
        }
    }
}

@Composable
private fun LiveChannelsCategoryBar(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    channelCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIVE CHANNELS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8E9B9E),
                letterSpacing = 1.sp
            )

            Text(
                text = "$channelCount Streams",
                fontSize = 11.sp,
                color = Color(0xFFBAC9CC)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allList = listOf("All") + categories.filter { it != "All" }
            items(allList) { category ->
                val isSelected = category == selectedCategory
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) CyanPrimary else DarkSurfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) CyanPrimary else Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelectCategory(category) }
                        .testTag("home_category_chip_$category")
                ) {
                    Text(
                        text = category,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF00363D) else Color(0xFFDAE2FD),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeChannelItem(
    channel: PlaylistChannel,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onPlay() }
            .testTag("home_channel_item_${channel.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Channel Logo / Fallback Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (!channel.tvgLogo.isNullOrBlank()) {
                        AsyncImage(
                            model = channel.tvgLogo,
                            contentDescription = channel.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = channel.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AccentWarning)
                        )
                        Text(
                            text = channel.categoryName ?: "Live",
                            fontSize = 11.sp,
                            color = Color(0xFFBAC9CC)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("home_channel_fav_${channel.id}")
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (channel.isFavorite) Color(0xFFF43F5E) else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = CyanPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = CyanPrimary,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChannelsState(
    onSync: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Live Channels Loaded",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sync with the default GitHub playlist to fetch 230+ live streams instantly.",
                fontSize = 12.sp,
                color = Color(0xFFBAC9CC),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSync,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = Color(0xFF00363D)
                ),
                modifier = Modifier.testTag("empty_channels_sync_button")
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync GitHub Playlist Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}
