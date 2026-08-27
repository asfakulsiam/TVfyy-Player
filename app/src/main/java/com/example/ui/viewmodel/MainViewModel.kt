package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TvFyyDatabase
import com.example.data.local.entity.FavoriteEntity
import com.example.data.local.entity.PlaybackHistoryEntity
import com.example.data.repository.TvFyyRepository
import com.example.domain.model.MediaItemData
import com.example.domain.model.StreamDiagnostics
import com.example.domain.model.StreamType
import com.example.domain.model.UrlProfile
import com.example.resolver.MediaTypeDetector
import com.example.resolver.UrlAnalyzer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UrlScreenState(
    val url: String = "",
    val detectedType: StreamType = StreamType.UNKNOWN,
    val isAnalyzing: Boolean = false,
    val userAgent: String = "",
    val referer: String = "",
    val authorization: String = "",
    val cookies: String = "",
    val customHeaders: List<Pair<String, String>> = emptyList(),
    val selectedProfileId: Long? = null,
    val errorMessage: String? = null,
    val diagnosticsResult: StreamDiagnostics? = null,
    val showAdvancedHeaders: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TvFyyRepository(TvFyyDatabase.getDatabase(application))

    val recentHistory: StateFlow<List<PlaybackHistoryEntity>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHistory: StateFlow<List<PlaybackHistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFavorites: StateFlow<List<FavoriteEntity>> = repository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProfiles: StateFlow<List<UrlProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _urlState = MutableStateFlow(UrlScreenState())
    val urlState: StateFlow<UrlScreenState> = _urlState.asStateFlow()

    private val _navigateToPlayer = MutableSharedFlow<MediaItemData>()
    val navigateToPlayer: SharedFlow<MediaItemData> = _navigateToPlayer.asSharedFlow()

    fun onUrlChanged(newUrl: String) {
        val detected = MediaTypeDetector.detectFromExtension(newUrl)
        _urlState.value = _urlState.value.copy(
            url = newUrl,
            detectedType = detected,
            errorMessage = null
        )
    }

    fun onUserAgentChanged(ua: String) {
        _urlState.value = _urlState.value.copy(userAgent = ua)
    }

    fun onRefererChanged(ref: String) {
        _urlState.value = _urlState.value.copy(referer = ref)
    }

    fun onAuthChanged(auth: String) {
        _urlState.value = _urlState.value.copy(authorization = auth)
    }

    fun onCookiesChanged(cookies: String) {
        _urlState.value = _urlState.value.copy(cookies = cookies)
    }

    fun toggleAdvancedHeaders() {
        _urlState.value = _urlState.value.copy(
            showAdvancedHeaders = !_urlState.value.showAdvancedHeaders
        )
    }

    fun addCustomHeader(key: String, value: String) {
        if (key.isNotBlank()) {
            val updated = _urlState.value.customHeaders.toMutableList()
            updated.add(key to value)
            _urlState.value = _urlState.value.copy(customHeaders = updated)
        }
    }

    fun removeCustomHeader(index: Int) {
        val updated = _urlState.value.customHeaders.toMutableList()
        if (index in updated.indices) {
            updated.removeAt(index)
            _urlState.value = _urlState.value.copy(customHeaders = updated)
        }
    }

    fun applyProfile(profile: UrlProfile) {
        _urlState.value = _urlState.value.copy(
            selectedProfileId = profile.id,
            userAgent = profile.userAgent ?: "",
            referer = profile.referer ?: "",
            authorization = profile.authorization ?: "",
            cookies = profile.cookies ?: "",
            customHeaders = profile.customHeaders.map { it.key to it.value },
            showAdvancedHeaders = true
        )
    }

    fun playUrl(overrideUrl: String? = null, title: String? = null) {
        val targetUrl = (overrideUrl ?: _urlState.value.url).trim()
        if (targetUrl.isBlank()) {
            _urlState.value = _urlState.value.copy(errorMessage = "Please enter a valid video stream URL.")
            return
        }

        viewModelScope.launch {
            _urlState.value = _urlState.value.copy(isAnalyzing = true, errorMessage = null)
            val headersMap = buildCurrentHeadersMap()

            val analysis = repository.analyzeUrl(targetUrl, headersMap)
            _urlState.value = _urlState.value.copy(
                isAnalyzing = false,
                detectedType = analysis.streamType,
                diagnosticsResult = analysis.diagnostics
            )

            if (analysis.isPlayable) {
                val mediaTitle = title ?: extractTitleFromUrl(analysis.effectiveUrl)
                val mediaItemData = MediaItemData(
                    uri = analysis.effectiveUrl,
                    title = mediaTitle,
                    streamType = analysis.streamType,
                    headers = headersMap,
                    isLocalFile = analysis.effectiveUrl.startsWith("content://") || analysis.effectiveUrl.startsWith("file://")
                )
                _navigateToPlayer.emit(mediaItemData)
            } else {
                _urlState.value = _urlState.value.copy(
                    errorMessage = analysis.userErrorMessage ?: "Unable to stream video from this URL."
                )
            }
        }
    }

    fun runDiagnostics() {
        val targetUrl = _urlState.value.url.trim()
        if (targetUrl.isBlank()) {
            _urlState.value = _urlState.value.copy(errorMessage = "Please enter a URL to run diagnostics.")
            return
        }

        viewModelScope.launch {
            _urlState.value = _urlState.value.copy(isAnalyzing = true, errorMessage = null)
            val headers = buildCurrentHeadersMap()
            val analysis = repository.analyzeUrl(targetUrl, headers)
            _urlState.value = _urlState.value.copy(
                isAnalyzing = false,
                detectedType = analysis.streamType,
                diagnosticsResult = analysis.diagnostics
            )
        }
    }

    fun dismissDiagnostics() {
        _urlState.value = _urlState.value.copy(diagnosticsResult = null)
    }

    fun onLocalMediaSelected(uri: Uri) {
        val context = getApplication<Application>()
        var fileName = "Local Video"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }
        } catch (_: Exception) {}

        val streamType = MediaTypeDetector.detectFromExtension(fileName)
        val mediaItemData = MediaItemData(
            uri = uri.toString(),
            title = fileName,
            streamType = if (streamType != StreamType.UNKNOWN) streamType else StreamType.PROGRESSIVE,
            isLocalFile = true
        )
        viewModelScope.launch {
            _navigateToPlayer.emit(mediaItemData)
        }
    }

    fun playHistoryItem(item: PlaybackHistoryEntity) {
        val streamType = MediaTypeDetector.detectFromExtension(item.url)
        val mediaItem = MediaItemData(
            uri = item.url,
            title = item.title,
            streamType = if (streamType != StreamType.UNKNOWN) streamType else StreamType.PROGRESSIVE,
            resumePositionMs = item.lastPositionMs,
            isLocalFile = item.isLocal
        )
        viewModelScope.launch {
            _navigateToPlayer.emit(mediaItem)
        }
    }

    fun playFavoriteItem(item: FavoriteEntity) {
        val headers = mutableMapOf<String, String>()
        item.userAgent?.let { headers["User-Agent"] = it }
        item.referer?.let { headers["Referer"] = it }
        item.authorization?.let { headers["Authorization"] = it }

        val streamType = MediaTypeDetector.detectFromExtension(item.url)
        val mediaItem = MediaItemData(
            uri = item.url,
            title = item.title,
            streamType = if (streamType != StreamType.UNKNOWN) streamType else StreamType.PROGRESSIVE,
            headers = headers
        )
        viewModelScope.launch {
            _navigateToPlayer.emit(mediaItem)
        }
    }

    fun toggleFavorite(title: String, url: String, mediaType: String) {
        viewModelScope.launch {
            val headers = buildCurrentHeadersMap()
            repository.toggleFavorite(title, url, mediaType, headers)
        }
    }

    fun playStream(url: String, title: String, streamType: StreamType = StreamType.HLS) {
        viewModelScope.launch {
            val mediaItemData = MediaItemData(
                uri = url,
                title = title,
                streamType = streamType,
                headers = emptyMap(),
                isLocalFile = url.startsWith("content://") || url.startsWith("file://")
            )
            _navigateToPlayer.emit(mediaItemData)
        }
    }

    fun deleteFavorite(id: Long) {
        viewModelScope.launch {
            repository.deleteFavorite(id)
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun saveProfile(profile: UrlProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
        }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch {
            repository.deleteProfile(id)
        }
    }

    private fun buildCurrentHeadersMap(): Map<String, String> {
        val state = _urlState.value
        val map = mutableMapOf<String, String>()
        if (state.userAgent.isNotBlank()) map["User-Agent"] = state.userAgent
        if (state.referer.isNotBlank()) map["Referer"] = state.referer
        if (state.authorization.isNotBlank()) map["Authorization"] = state.authorization
        if (state.cookies.isNotBlank()) map["Cookie"] = state.cookies
        state.customHeaders.forEach { (k, v) ->
            if (k.isNotBlank() && v.isNotBlank()) {
                map[k] = v
            }
        }
        return map
    }

    private fun extractTitleFromUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            val lastSegment = uri.lastPathSegment
            if (!lastSegment.isNullOrBlank()) {
                lastSegment.substringBeforeLast(".")
                    .replace("-", " ")
                    .replace("_", " ")
                    .capitalizeWord()
            } else {
                "Online Stream"
            }
        } catch (_: Exception) {
            "Online Stream"
        }
    }

    private fun String.capitalizeWord(): String {
        return split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}
