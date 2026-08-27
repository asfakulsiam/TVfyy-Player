package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TvFyyDatabase
import com.example.data.parser.M3uExportOptions
import com.example.data.repository.PlaylistRepository
import com.example.domain.model.ChannelSortOrder
import com.example.domain.model.ImportMode
import com.example.domain.model.MediaItemData
import com.example.domain.model.ParsedPlaylist
import com.example.domain.model.Playlist
import com.example.domain.model.PlaylistCategory
import com.example.domain.model.PlaylistChangeSummary
import com.example.domain.model.PlaylistChannel
import com.example.domain.model.PlaylistSourceType
import com.example.domain.model.StreamType
import com.example.resolver.MediaTypeDetector
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.OutputStream

enum class ImportSourceTab(val title: String) {
    FILE("Local File"),
    URL("URL"),
    GITHUB("GitHub / Raw"),
    PASTE("Paste M3U")
}

data class ImportUiState(
    val selectedTab: ImportSourceTab = ImportSourceTab.URL,
    val playlistName: String = "",
    val urlInput: String = "",
    val pastedText: String = "",
    val selectedFileUri: Uri? = null,
    val selectedFileName: String? = null,
    val isParsing: Boolean = false,
    val parsedPreview: ParsedPlaylist? = null,
    val errorMessage: String? = null,
    val importMode: ImportMode = ImportMode.ADD_AS_NEW
)

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlaylistRepository(TvFyyDatabase.getDatabase(application))

    val playlists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Playlist Detail State
    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(ChannelSortOrder.CUSTOM)
    val sortOrder: StateFlow<ChannelSortOrder> = _sortOrder.asStateFlow()

    private val _filterFavorites = MutableStateFlow(false)
    val filterFavorites: StateFlow<Boolean> = _filterFavorites.asStateFlow()

    private val _selectedChannelIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedChannelIds: StateFlow<Set<Long>> = _selectedChannelIds.asStateFlow()

    private val _isBulkSelectionMode = MutableStateFlow(false)
    val isBulkSelectionMode: StateFlow<Boolean> = _isBulkSelectionMode.asStateFlow()

    // Import State
    private val _importState = MutableStateFlow(ImportUiState())
    val importState: StateFlow<ImportUiState> = _importState.asStateFlow()

    // Refresh Change Summary
    private val _refreshSummary = MutableStateFlow<PlaylistChangeSummary?>(null)
    val refreshSummary: StateFlow<PlaylistChangeSummary?> = _refreshSummary.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Undo Cache
    private var lastDeletedChannel: PlaylistChannel? = null
    private var lastDeletedChannels: List<PlaylistChannel>? = null
    private var lastDeletedPlaylist: Playlist? = null

    // Navigation & Messages
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _playChannel = MutableSharedFlow<MediaItemData>()
    val playChannel: SharedFlow<MediaItemData> = _playChannel.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<PlaylistCategory>> = _selectedPlaylistId.flatMapLatest { id ->
        if (id != null) repository.getCategoriesFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawChannels: StateFlow<List<PlaylistChannel>> = _selectedPlaylistId.flatMapLatest { id ->
        if (id != null) repository.getChannelsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayedChannels: StateFlow<List<PlaylistChannel>> = combine(
        rawChannels,
        _selectedCategory,
        _searchQuery,
        _sortOrder,
        _filterFavorites
    ) { channels, category, query, sort, onlyFavs ->
        var list = channels

        // 1. Filter by category
        if (category != "All") {
            list = list.filter { it.categoryName.equals(category, ignoreCase = true) }
        }

        // 2. Filter by favorites
        if (onlyFavs) {
            list = list.filter { it.isFavorite }
        }

        // 3. Filter by search query (case-insensitive Unicode support)
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                        (it.tvgName?.lowercase()?.contains(q) == true) ||
                        (it.tvgId?.lowercase()?.contains(q) == true) ||
                        it.categoryName.lowercase().contains(q)
            }
        }

        // 4. Sort
        when (sort) {
            ChannelSortOrder.CUSTOM -> list.sortedBy { it.position }
            ChannelSortOrder.NAME_ASC -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            ChannelSortOrder.NAME_DESC -> list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
            ChannelSortOrder.RECENTLY_ADDED -> list.sortedByDescending { it.createdAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectPlaylist(id: Long?) {
        _selectedPlaylistId.value = id
        _selectedCategory.value = "All"
        _searchQuery.value = ""
        _selectedChannelIds.value = emptySet()
        _isBulkSelectionMode.value = false
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: ChannelSortOrder) {
        _sortOrder.value = order
    }

    fun toggleFilterFavorites() {
        _filterFavorites.value = !_filterFavorites.value
    }

    // Bulk selection
    fun toggleBulkSelectionMode() {
        _isBulkSelectionMode.value = !_isBulkSelectionMode.value
        if (!_isBulkSelectionMode.value) {
            _selectedChannelIds.value = emptySet()
        }
    }

    fun toggleChannelSelection(channelId: Long) {
        val current = _selectedChannelIds.value.toMutableSet()
        if (current.contains(channelId)) {
            current.remove(channelId)
        } else {
            current.add(channelId)
        }
        _selectedChannelIds.value = current
    }

    fun selectAllDisplayedChannels() {
        val allIds = displayedChannels.value.map { it.id }.toSet()
        _selectedChannelIds.value = allIds
    }

    fun clearChannelSelection() {
        _selectedChannelIds.value = emptySet()
    }

    // Import Flow
    fun setImportTab(tab: ImportSourceTab) {
        _importState.value = _importState.value.copy(
            selectedTab = tab,
            errorMessage = null
        )
    }

    fun onImportPlaylistNameChanged(name: String) {
        _importState.value = _importState.value.copy(playlistName = name)
    }

    fun onImportUrlChanged(url: String) {
        _importState.value = _importState.value.copy(urlInput = url, errorMessage = null)
    }

    fun onImportPastedTextChanged(text: String) {
        _importState.value = _importState.value.copy(pastedText = text, errorMessage = null)
    }

    fun onImportModeChanged(mode: ImportMode) {
        _importState.value = _importState.value.copy(importMode = mode)
    }

    fun onFileSelected(uri: Uri, fileName: String?) {
        _importState.value = _importState.value.copy(
            selectedFileUri = uri,
            selectedFileName = fileName,
            playlistName = if (_importState.value.playlistName.isBlank()) fileName?.substringBeforeLast(".") ?: "My Playlist" else _importState.value.playlistName,
            errorMessage = null
        )
    }

    fun parseImportSource() {
        val state = _importState.value
        viewModelScope.launch {
            _importState.value = _importState.value.copy(isParsing = true, errorMessage = null, parsedPreview = null)
            when (state.selectedTab) {
                ImportSourceTab.FILE -> {
                    val uri = state.selectedFileUri
                    if (uri == null) {
                        _importState.value = _importState.value.copy(isParsing = false, errorMessage = "Please select a local M3U/M3U8 file first.")
                        return@launch
                    }
                    val result = repository.parseFromUri(getApplication(), uri)
                    result.fold(
                        onSuccess = { parsed ->
                            _importState.value = _importState.value.copy(
                                isParsing = false,
                                parsedPreview = parsed,
                                playlistName = if (_importState.value.playlistName.isBlank()) parsed.defaultName ?: "My Playlist" else _importState.value.playlistName
                            )
                        },
                        onFailure = { err ->
                            _importState.value = _importState.value.copy(isParsing = false, errorMessage = err.message ?: "Failed to parse selected file.")
                        }
                    )
                }
                ImportSourceTab.URL, ImportSourceTab.GITHUB -> {
                    val url = state.urlInput.trim()
                    if (url.isBlank() || (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true))) {
                        _importState.value = _importState.value.copy(isParsing = false, errorMessage = "Please enter a valid HTTP/HTTPS playlist URL.")
                        return@launch
                    }
                    val result = repository.downloadAndParseUrl(url, state.playlistName.ifBlank { null })
                    result.fold(
                        onSuccess = { parsed ->
                            _importState.value = _importState.value.copy(
                                isParsing = false,
                                parsedPreview = parsed,
                                playlistName = if (_importState.value.playlistName.isBlank()) parsed.defaultName ?: "Remote Playlist" else _importState.value.playlistName
                            )
                        },
                        onFailure = { err ->
                            _importState.value = _importState.value.copy(isParsing = false, errorMessage = err.message ?: "Failed to download and parse remote playlist.")
                        }
                    )
                }
                ImportSourceTab.PASTE -> {
                    val text = state.pastedText.trim()
                    if (text.isBlank()) {
                        _importState.value = _importState.value.copy(isParsing = false, errorMessage = "Please paste valid M3U text content.")
                        return@launch
                    }
                    val parsed = repository.parseFromText(text, state.playlistName.ifBlank { "Pasted Playlist" })
                    if (parsed.entries.isEmpty()) {
                        _importState.value = _importState.value.copy(isParsing = false, errorMessage = "No valid channels found in pasted text.")
                    } else {
                        _importState.value = _importState.value.copy(
                            isParsing = false,
                            parsedPreview = parsed,
                            playlistName = if (_importState.value.playlistName.isBlank()) "Pasted Playlist" else _importState.value.playlistName
                        )
                    }
                }
            }
        }
    }

    fun confirmImport(onSuccess: (Long) -> Unit) {
        val state = _importState.value
        val parsed = state.parsedPreview ?: return

        viewModelScope.launch {
            _importState.value = _importState.value.copy(isParsing = true)

            val sourceType = when (state.selectedTab) {
                ImportSourceTab.FILE -> PlaylistSourceType.LOCAL_FILE
                ImportSourceTab.URL -> PlaylistSourceType.REMOTE_URL
                ImportSourceTab.GITHUB -> PlaylistSourceType.GITHUB_RAW
                ImportSourceTab.PASTE -> PlaylistSourceType.PASTED_TEXT
            }
            val sourceUrl = if (sourceType == PlaylistSourceType.REMOTE_URL || sourceType == PlaylistSourceType.GITHUB_RAW) state.urlInput.trim() else null

            val result = repository.importParsedPlaylist(
                parsed = parsed,
                playlistName = state.playlistName.ifBlank { "New Playlist" },
                sourceType = sourceType,
                sourceUrl = sourceUrl,
                mode = state.importMode,
                targetPlaylistId = _selectedPlaylistId.value
            )

            _importState.value = _importState.value.copy(isParsing = false)

            result.fold(
                onSuccess = { newId ->
                    _importState.value = ImportUiState() // reset
                    _userMessage.emit("Playlist imported successfully (${parsed.totalValid} channels)")
                    onSuccess(newId)
                },
                onFailure = { err ->
                    _importState.value = _importState.value.copy(errorMessage = err.message ?: "Failed to import playlist.")
                }
            )
        }
    }

    fun resetImportState() {
        _importState.value = ImportUiState()
    }

    // Refresh Remote Playlist
    fun checkPlaylistRefresh(playlistId: Long) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.computeRefreshChanges(playlistId)
            _isRefreshing.value = false
            result.fold(
                onSuccess = { summary ->
                    _refreshSummary.value = summary
                },
                onFailure = { err ->
                    _userMessage.emit(err.message ?: "Failed to refresh remote playlist")
                }
            )
        }
    }

    fun applyRefreshUpdate(keepLocalEdits: Boolean = true) {
        val summary = _refreshSummary.value ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.applyRefreshChanges(summary.playlistId, keepLocalEdits)
            _isRefreshing.value = false
            _refreshSummary.value = null
            result.fold(
                onSuccess = {
                    _userMessage.emit("Playlist refreshed successfully (${summary.addedCount} added, ${summary.removedCount} removed, ${summary.modifiedCount} updated)")
                },
                onFailure = { err ->
                    _userMessage.emit(err.message ?: "Failed to apply playlist update")
                }
            )
        }
    }

    fun dismissRefreshSummary() {
        _refreshSummary.value = null
    }

    // Channel Actions
    fun playChannel(channel: PlaylistChannel) {
        viewModelScope.launch {
            val detectedType = MediaTypeDetector.detectFromExtension(channel.streamUrl)
            val mediaItem = MediaItemData(
                uri = channel.streamUrl,
                title = channel.name,
                streamType = if (detectedType != StreamType.UNKNOWN) detectedType else StreamType.HLS,
                isLocalFile = channel.streamUrl.startsWith("content://") || channel.streamUrl.startsWith("file://")
            )
            _playChannel.emit(mediaItem)
        }
    }

    fun saveChannel(channel: PlaylistChannel) {
        viewModelScope.launch {
            val result = repository.saveChannel(channel)
            result.fold(
                onSuccess = { _userMessage.emit("Channel saved") },
                onFailure = { err -> _userMessage.emit("Failed to save channel: ${err.message}") }
            )
        }
    }

    fun duplicateChannel(channelId: Long) {
        viewModelScope.launch {
            val result = repository.duplicateChannel(channelId)
            result.fold(
                onSuccess = { _userMessage.emit("Channel duplicated") },
                onFailure = { err -> _userMessage.emit("Failed to duplicate channel: ${err.message}") }
            )
        }
    }

    fun deleteChannel(channel: PlaylistChannel) {
        lastDeletedChannel = channel
        viewModelScope.launch {
            repository.deleteChannel(channel.id)
            _userMessage.emit("Channel '${channel.name}' deleted")
        }
    }

    fun undoDeleteChannel() {
        val channel = lastDeletedChannel ?: return
        viewModelScope.launch {
            repository.saveChannel(channel.copy(id = 0))
            lastDeletedChannel = null
            _userMessage.emit("Channel restored")
        }
    }

    fun bulkDeleteSelectedChannels() {
        val ids = _selectedChannelIds.value.toList()
        if (ids.isEmpty()) return
        val currentChannels = rawChannels.value.filter { ids.contains(it.id) }
        lastDeletedChannels = currentChannels

        viewModelScope.launch {
            repository.bulkDeleteChannels(ids)
            _selectedChannelIds.value = emptySet()
            _isBulkSelectionMode.value = false
            _userMessage.emit("${ids.size} channels deleted")
        }
    }

    fun undoBulkDelete() {
        val channels = lastDeletedChannels ?: return
        viewModelScope.launch {
            channels.forEach { repository.saveChannel(it.copy(id = 0)) }
            lastDeletedChannels = null
            _userMessage.emit("${channels.size} channels restored")
        }
    }

    fun moveChannelCategory(channelId: Long, newCategoryId: Long?, newCategoryName: String) {
        viewModelScope.launch {
            repository.moveChannelCategory(channelId, newCategoryId, newCategoryName)
            _userMessage.emit("Moved to $newCategoryName")
        }
    }

    fun bulkMoveCategory(newCategoryId: Long?, newCategoryName: String) {
        val ids = _selectedChannelIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.bulkMoveCategory(ids, newCategoryId, newCategoryName)
            _selectedChannelIds.value = emptySet()
            _isBulkSelectionMode.value = false
            _userMessage.emit("${ids.size} channels moved to $newCategoryName")
        }
    }

    fun toggleChannelFavorite(channel: PlaylistChannel) {
        viewModelScope.launch {
            repository.toggleFavorite(channel.id, !channel.isFavorite)
        }
    }

    fun moveChannelPosition(fromIndex: Int, toIndex: Int) {
        val current = displayedChannels.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            viewModelScope.launch {
                repository.reorderChannels(current)
            }
        }
    }

    // Category Actions
    fun addCategory(playlistId: Long, name: String) {
        viewModelScope.launch {
            val result = repository.addCategory(playlistId, name)
            result.fold(
                onSuccess = { _userMessage.emit("Category '$name' created") },
                onFailure = { err -> _userMessage.emit("Failed: ${err.message}") }
            )
        }
    }

    fun renameCategory(playlistId: Long, categoryId: Long, newName: String) {
        viewModelScope.launch {
            val result = repository.renameCategory(playlistId, categoryId, newName)
            result.fold(
                onSuccess = { _userMessage.emit("Category renamed to '$newName'") },
                onFailure = { err -> _userMessage.emit("Failed: ${err.message}") }
            )
        }
    }

    fun mergeCategories(playlistId: Long, fromCat: String, toCat: String) {
        viewModelScope.launch {
            val result = repository.mergeCategories(playlistId, fromCat, toCat)
            result.fold(
                onSuccess = { _userMessage.emit("Merged '$fromCat' into '$toCat'") },
                onFailure = { err -> _userMessage.emit("Failed: ${err.message}") }
            )
        }
    }

    fun deleteCategory(playlistId: Long, categoryId: Long, deleteChannels: Boolean) {
        viewModelScope.launch {
            val result = repository.deleteCategory(playlistId, categoryId, deleteChannels)
            result.fold(
                onSuccess = { _userMessage.emit("Category deleted") },
                onFailure = { err -> _userMessage.emit("Failed: ${err.message}") }
            )
        }
    }

    fun moveCategoryPosition(playlistId: Long, fromIndex: Int, toIndex: Int) {
        val current = categories.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            viewModelScope.launch {
                repository.reorderCategories(playlistId, current)
            }
        }
    }

    // Playlist Actions
    fun renamePlaylist(id: Long, newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(id, newName)
            _userMessage.emit("Playlist renamed")
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        lastDeletedPlaylist = playlist
        viewModelScope.launch {
            repository.deletePlaylist(playlist.id)
            if (_selectedPlaylistId.value == playlist.id) {
                _selectedPlaylistId.value = null
            }
            _userMessage.emit("Playlist '${playlist.name}' deleted")
        }
    }

    fun duplicatePlaylist(id: Long) {
        viewModelScope.launch {
            val result = repository.duplicatePlaylist(id)
            result.fold(
                onSuccess = { _userMessage.emit("Playlist duplicated") },
                onFailure = { err -> _userMessage.emit("Failed to duplicate: ${err.message}") }
            )
        }
    }

    fun setActivePlaylist(id: Long) {
        viewModelScope.launch {
            repository.setActivePlaylist(id)
            _userMessage.emit("Playlist set as active")
        }
    }

    // Export Action
    fun exportPlaylistToStream(
        outputStream: OutputStream,
        playlistId: Long,
        options: M3uExportOptions,
        categoryFilter: String? = null,
        selectedChannelIds: List<Long>? = null,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.exportPlaylistToStream(
                    outputStream = outputStream,
                    playlistId = playlistId,
                    options = options,
                    categoryFilter = categoryFilter,
                    selectedChannelIds = selectedChannelIds
                )
                _userMessage.emit("Playlist exported successfully")
                onComplete(true)
            } catch (e: Exception) {
                _userMessage.emit("Export failed: ${e.message}")
                onComplete(false)
            }
        }
    }

    suspend fun getExportString(
        playlistId: Long,
        options: M3uExportOptions = M3uExportOptions(),
        categoryFilter: String? = null,
        selectedChannelIds: List<Long>? = null
    ): String {
        return repository.exportPlaylistToString(playlistId, options, categoryFilter, selectedChannelIds)
    }
}
