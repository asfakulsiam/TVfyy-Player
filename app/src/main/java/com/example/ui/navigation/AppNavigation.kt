package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.MediaItemData
import com.example.ui.favorites.FavoritesScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.player.VideoPlayerScreen
import com.example.ui.player.components.MiniPlayer
import com.example.ui.playlist.PlaylistDetailScreen
import com.example.ui.playlist.PlaylistsScreen
import com.example.ui.profiles.ProfilesScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.support.BuyMeACoffeeScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.url.UrlStreamScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.update.UpdateDialog
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PlayerViewModel
import com.example.ui.viewmodel.PlaylistViewModel
import com.example.ui.viewmodel.UpdateViewModel

sealed class NavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : NavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Playlists : NavItem("playlists", "Playlists", Icons.Filled.PlaylistPlay, Icons.Outlined.PlaylistPlay)
    data object Url : NavItem("url", "Stream", Icons.Filled.Link, Icons.Outlined.Link)
    data object Favorites : NavItem("favorites", "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)
    data object History : NavItem("history", "History", Icons.Filled.History, Icons.Outlined.History)
    data object Profiles : NavItem("profiles", "Profiles", Icons.Filled.Security, Icons.Outlined.Security)
    data object Settings : NavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    updateViewModel: UpdateViewModel,
    modifier: Modifier = Modifier
) {
    var isSplashVisible by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf<NavItem>(NavItem.Home) }
    var activeMediaItem by remember { mutableStateOf<MediaItemData?>(null) }
    var openedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var isSupportOpen by remember { mutableStateOf(false) }

    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val updateState by updateViewModel.updateState.collectAsStateWithLifecycle()
    val showUpdateDialog by updateViewModel.showUpdateDialog.collectAsStateWithLifecycle()

    if (isSplashVisible) {
        SplashScreen(
            onSplashFinished = { isSplashVisible = false }
        )
        return
    }

    val navItems = listOf(
        NavItem.Home,
        NavItem.Playlists,
        NavItem.Url,
        NavItem.Favorites,
        NavItem.History,
        NavItem.Profiles,
        NavItem.Settings
    )

    LaunchedEffect(Unit) {
        mainViewModel.navigateToPlayer.collect { mediaItem ->
            activeMediaItem = mediaItem
            isSupportOpen = false
            playerViewModel.maximizeFromMiniPlayer()
        }
    }

    LaunchedEffect(Unit) {
        playlistViewModel.playChannel.collect { mediaItem ->
            activeMediaItem = mediaItem
            isSupportOpen = false
            playerViewModel.maximizeFromMiniPlayer()
        }
    }

    if (showUpdateDialog && updateState.updateInfo != null) {
        val currentUpdate = updateState.updateInfo!!
        UpdateDialog(
            updateInfo = currentUpdate,
            onDownloadNow = { updateViewModel.onDownloadNow(currentUpdate) },
            onRemindLater = { updateViewModel.onRemindLater(currentUpdate) },
            onDismiss = { updateViewModel.dismissDialog() }
        )
    }

    if (activeMediaItem != null && !playerUiState.isMiniPlayerActive) {
        VideoPlayerScreen(
            mediaItemData = activeMediaItem!!,
            viewModel = playerViewModel,
            onBack = {
                playerViewModel.minimizeToMiniPlayer()
            },
            onOpenSupport = {
                playerViewModel.minimizeToMiniPlayer()
                isSupportOpen = true
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentTab == item && openedPlaylistId == null && !isSupportOpen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                openedPlaylistId = null
                                isSupportOpen = false
                                currentTab = item
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF00363D),
                                selectedTextColor = CyanPrimary,
                                indicatorColor = CyanPrimary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_${item.route}")
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isSupportOpen) {
                    BuyMeACoffeeScreen(
                        onBack = { isSupportOpen = false }
                    )
                } else if (openedPlaylistId != null) {
                    PlaylistDetailScreen(
                        playlistId = openedPlaylistId!!,
                        viewModel = playlistViewModel,
                        onBack = { openedPlaylistId = null }
                    )
                } else {
                    when (currentTab) {
                        NavItem.Home -> HomeScreen(
                            viewModel = mainViewModel,
                            onNavigateToUrl = { currentTab = NavItem.Url },
                            onNavigateToPlaylists = { currentTab = NavItem.Playlists },
                            onNavigateToFavorites = { currentTab = NavItem.Favorites },
                            onNavigateToSupport = { isSupportOpen = true },
                            onCheckForUpdates = { updateViewModel.checkForUpdates(isUserInitiated = true) },
                            availableUpdate = updateState.updateInfo
                        )
                        NavItem.Playlists -> PlaylistsScreen(
                            viewModel = playlistViewModel,
                            onOpenPlaylist = { id -> openedPlaylistId = id }
                        )
                        NavItem.Url -> UrlStreamScreen(viewModel = mainViewModel)
                        NavItem.Favorites -> FavoritesScreen(viewModel = mainViewModel)
                        NavItem.History -> HistoryScreen(viewModel = mainViewModel)
                        NavItem.Profiles -> ProfilesScreen(viewModel = mainViewModel)
                        NavItem.Settings -> SettingsScreen(
                            onNavigateToProfiles = { currentTab = NavItem.Profiles },
                            onNavigateToSupport = { isSupportOpen = true },
                            onCheckForUpdates = { updateViewModel.checkForUpdates(isUserInitiated = true) },
                            updateState = updateState,
                            autoCheckUpdates = updateViewModel.isAutoCheckEnabled(),
                            onToggleAutoCheckUpdates = { updateViewModel.setAutoCheckEnabled(it) }
                        )
                    }
                }

                // Docked In-App MiniPlayer floating above bottom bar
                AnimatedVisibility(
                    visible = activeMediaItem != null && playerUiState.isMiniPlayerActive,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    if (activeMediaItem != null) {
                        MiniPlayer(
                            mediaItem = activeMediaItem!!,
                            playbackState = playbackState,
                            onMaximize = {
                                playerViewModel.maximizeFromMiniPlayer()
                            },
                            onTogglePlayPause = {
                                playerViewModel.togglePlayPause()
                            },
                            onClose = {
                                playerViewModel.closeMiniPlayer()
                                activeMediaItem = null
                            }
                        )
                    }
                }
            }
        }
    }
}
