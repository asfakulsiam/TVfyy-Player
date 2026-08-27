package com.example.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import com.example.domain.model.MediaItemData
import com.example.domain.model.PlaybackState
import com.example.domain.model.StreamDiagnostics
import com.example.domain.model.StreamType
import com.example.domain.model.SubtitleSource
import com.example.domain.model.TrackInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

@UnstableApi
class Media3PlayerEngine(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : PlayerEngine {

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _videoTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    override val videoTracks: StateFlow<List<TrackInfo>> = _videoTracks.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    override val audioTracks: StateFlow<List<TrackInfo>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    override val subtitleTracks: StateFlow<List<TrackInfo>> = _subtitleTracks.asStateFlow()

    private val _streamDiagnostics = MutableStateFlow(StreamDiagnostics())
    override val streamDiagnostics: StateFlow<StreamDiagnostics> = _streamDiagnostics.asStateFlow()

    private val _activeCuesText = MutableStateFlow<String?>(null)
    override val activeCuesText: StateFlow<String?> = _activeCuesText.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var trackManager: TrackManager? = null
    private var currentMediaItemData: MediaItemData? = null
    private val dynamicallyAddedSubtitles = mutableListOf<TrackInfo>()
    private var progressJob: Job? = null
    private var reconnectJob: Job? = null

    private var currentVolumeBoostPercent: Int = 100
    private var baseVolume: Float = 1.0f
    private var audioDelayMs: Long = 0L
    private var subtitleDelayMs: Long = 0L
    private var retryCount = 0
    private val maxRetries = 3

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        val trackSelectorInstance = DefaultTrackSelector(context)
        this.trackSelector = trackSelectorInstance
        this.trackManager = TrackManager(trackSelectorInstance)

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000, // min buffer 15s
                50_000, // max buffer 50s
                1_500,  // buffer for playback
                2_500   // buffer for playback after rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelectorInstance)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true // handle audio focus automatically
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    retryCount = 0
                    _playbackState.value = _playbackState.value.copy(
                        isReconnecting = false,
                        reconnectAttempt = 0
                    )
                }
                updatePlaybackState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                trackManager?.let { tm ->
                    _videoTracks.value = tm.extractVideoTracks(tracks)
                    _audioTracks.value = tm.extractAudioTracks(tracks)
                    _subtitleTracks.value = tm.extractSubtitleTracks(tracks, dynamicallyAddedSubtitles)
                    updateDiagnosticsFromTracks(tracks)
                }
            }

            override fun onCues(cueGroup: CueGroup) {
                if (cueGroup.cues.isEmpty()) {
                    _activeCuesText.value = null
                } else {
                    val combinedText = cueGroup.cues.mapNotNull { it.text?.toString() }.filter { it.isNotBlank() }.joinToString("\n")
                    _activeCuesText.value = combinedText.ifBlank { null }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                _playbackState.value = _playbackState.value.copy(
                    videoWidth = videoSize.width,
                    videoHeight = videoSize.height
                )
                _streamDiagnostics.value = _streamDiagnostics.value.copy(
                    resolution = if (videoSize.width > 0 && videoSize.height > 0) "${videoSize.width}x${videoSize.height}" else null
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                handlePlaybackError(error)
            }
        })

        // MediaSession integration
        try {
            this.mediaSession = MediaSession.Builder(context, player)
                .setId("TVfyyPlayerSession")
                .build()
        } catch (_: Exception) {}

        this.exoPlayer = player
    }

    override fun prepare(mediaItemData: MediaItemData) {
        this.currentMediaItemData = mediaItemData
        this.retryCount = 0
        this.dynamicallyAddedSubtitles.clear()
        val player = exoPlayer ?: return

        _playbackState.value = PlaybackState(
            isBuffering = true,
            volume = baseVolume,
            volumeBoostPercent = currentVolumeBoostPercent,
            audioDelayMs = audioDelayMs,
            subtitleDelayMs = subtitleDelayMs
        )
        _streamDiagnostics.value = StreamDiagnostics(
            url = mediaItemData.uri,
            reachable = true,
            detectedStreamType = mediaItemData.streamType,
            headersSent = mediaItemData.headers
        )

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        val userAgent = mediaItemData.headers["User-Agent"] ?: "TVfyyPlayer/1.0 (Android; ExoPlayer)"
        httpDataSourceFactory.setUserAgent(userAgent)

        val customHeaderMap = mediaItemData.headers.filterKeys { it != "User-Agent" }
        if (customHeaderMap.isNotEmpty()) {
            httpDataSourceFactory.setDefaultRequestProperties(customHeaderMap)
        }

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val metadata = MediaMetadata.Builder()
            .setTitle(mediaItemData.title)
            .setDisplayTitle(mediaItemData.title)
            .build()

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(mediaItemData.uri)
            .setMediaMetadata(metadata)

        // Subtitle configurations
        val subtitleConfigs = mutableListOf<MediaItem.SubtitleConfiguration>()
        mediaItemData.subtitleUri?.let { subUri ->
            val subtitleMime = when {
                subUri.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                subUri.endsWith(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
                subUri.endsWith(".ass", ignoreCase = true) || subUri.endsWith(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
                else -> MimeTypes.TEXT_VTT
            }
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subUri))
                .setMimeType(subtitleMime)
                .setLanguage("und")
                .setLabel("Initial Subtitle")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            subtitleConfigs.add(subtitleConfig)
        }

        if (subtitleConfigs.isNotEmpty()) {
            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
        }

        val mediaItem = mediaItemBuilder.build()

        val mediaSource: MediaSource = when (mediaItemData.streamType) {
            StreamType.HLS -> HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
            StreamType.DASH -> DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            StreamType.PROGRESSIVE, StreamType.MPEG_TS, StreamType.UNKNOWN -> {
                DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
            }
        }

        player.setMediaSource(mediaSource)
        player.prepare()

        if (mediaItemData.resumePositionMs > 0) {
            player.seekTo(mediaItemData.resumePositionMs)
        }

        player.playWhenReady = true
        startProgressTracker()
    }

    override fun addExternalSubtitleTrack(trackInfo: TrackInfo) {
        val player = exoPlayer ?: return
        val currentMedia = currentMediaItemData ?: return
        val filePath = trackInfo.filePath ?: return

        // Update list of dynamic subtitle tracks
        dynamicallyAddedSubtitles.removeAll { it.filePath == filePath }
        dynamicallyAddedSubtitles.add(trackInfo.copy(isSelected = true))

        val fileUri = Uri.fromFile(File(filePath))
        val mime = trackInfo.mimeType ?: MimeTypes.APPLICATION_SUBRIP

        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(fileUri)
            .setMimeType(mime)
            .setLabel(trackInfo.label)
            .setLanguage(trackInfo.languageCode ?: "und")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT or C.SELECTION_FLAG_FORCED)
            .build()

        // Preserve current playback state and position
        val currentPosition = player.currentPosition
        val wasPlaying = player.isPlaying

        val updatedMediaItem = MediaItem.Builder()
            .setUri(currentMedia.uri)
            .setSubtitleConfigurations(listOf(subtitleConfig))
            .build()

        val okHttpClient = OkHttpClient.Builder().build()
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val mediaSource = when (currentMedia.streamType) {
            StreamType.HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(updatedMediaItem)
            StreamType.DASH -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(updatedMediaItem)
            else -> DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(updatedMediaItem)
        }

        player.setMediaSource(mediaSource)
        player.prepare()
        if (currentPosition > 0) {
            player.seekTo(currentPosition)
        }
        player.playWhenReady = wasPlaying

        // Force enable text track
        trackSelector?.let { ts ->
            val builder = ts.buildUponParameters()
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            ts.setParameters(builder)
        }

        _subtitleTracks.value = trackManager?.extractSubtitleTracks(player.currentTracks, dynamicallyAddedSubtitles) ?: dynamicallyAddedSubtitles
    }

    override fun play() {
        exoPlayer?.play()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val duration = if (player.duration > 0 && player.duration != C.TIME_UNSET) player.duration else Long.MAX_VALUE
        val clamped = positionMs.coerceIn(0L, duration)
        player.seekTo(clamped)
        updatePlaybackState()
    }

    override fun seekBy(deltaMs: Long) {
        val player = exoPlayer ?: return
        val current = player.currentPosition
        val duration = if (player.duration > 0 && player.duration != C.TIME_UNSET) player.duration else Long.MAX_VALUE
        val target = (current + deltaMs).coerceIn(0L, duration)
        player.seekTo(target)
        updatePlaybackState()
    }

    override fun jumpToLiveEdge() {
        val player = exoPlayer ?: return
        if (player.isCurrentMediaItemLive) {
            val liveDuration = player.duration
            if (liveDuration > 0 && liveDuration != C.TIME_UNSET) {
                player.seekTo(liveDuration)
            } else {
                player.seekToDefaultPosition()
            }
            updatePlaybackState()
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        val player = exoPlayer ?: return
        val clamped = speed.coerceIn(0.25f, 3.0f)
        player.playbackParameters = PlaybackParameters(clamped)
        _playbackState.value = _playbackState.value.copy(playbackSpeed = clamped)
    }

    override fun selectTrack(trackInfo: TrackInfo?) {
        val player = exoPlayer ?: return
        val tm = trackManager ?: return
        if (trackInfo?.type == com.example.domain.model.TrackType.AUDIO) {
            tm.selectAudioTrack(player.currentTracks, trackInfo)
        } else {
            tm.selectVideoTrack(player.currentTracks, trackInfo)
        }
    }

    override fun selectAutoQuality() {
        selectTrack(null)
    }

    override fun selectSubtitleTrack(trackInfo: TrackInfo?) {
        val player = exoPlayer ?: return
        val tm = trackManager ?: return
        if (trackInfo != null && (trackInfo.source == SubtitleSource.EXTERNAL_FILE || trackInfo.source == SubtitleSource.ONLINE_DOWNLOAD || trackInfo.source == SubtitleSource.EXTERNAL_URL)) {
            addExternalSubtitleTrack(trackInfo)
        } else {
            tm.selectSubtitleTrack(player.currentTracks, trackInfo)
        }
    }

    override fun disableSubtitles() {
        selectSubtitleTrack(null)
        _activeCuesText.value = null
    }

    override fun setVolume(volume: Float) {
        val player = exoPlayer ?: return
        baseVolume = volume.coerceIn(0f, 1f)
        applyEffectiveVolume(player)
    }

    override fun setVolumeBoost(percent: Int) {
        val player = exoPlayer ?: return
        currentVolumeBoostPercent = percent.coerceIn(100, 150)
        applyEffectiveVolume(player)
    }

    private fun applyEffectiveVolume(player: Player) {
        val multiplier = currentVolumeBoostPercent / 100f
        val effective = (baseVolume * multiplier).coerceIn(0f, 1.5f)
        player.volume = (effective / 1.5f).coerceIn(0f, 1f)
        _playbackState.value = _playbackState.value.copy(
            volume = baseVolume,
            volumeBoostPercent = currentVolumeBoostPercent,
            isMuted = baseVolume == 0f
        )
    }

    override fun setAudioDelay(delayMs: Long) {
        audioDelayMs = delayMs.coerceIn(-5000L, 5000L)
        _playbackState.value = _playbackState.value.copy(audioDelayMs = audioDelayMs)
    }

    override fun setSubtitleDelay(delayMs: Long) {
        subtitleDelayMs = delayMs.coerceIn(-5000L, 5000L)
        _playbackState.value = _playbackState.value.copy(subtitleDelayMs = subtitleDelayMs)
    }

    override fun setPreferredLanguages(audioLang: String?, subtitleLang: String?) {
        trackManager?.applyPreferredLanguages(audioLang, subtitleLang)
    }

    override fun retry() {
        reconnectJob?.cancel()
        currentMediaItemData?.let { prepare(it) } ?: exoPlayer?.prepare()
    }

    private fun handlePlaybackError(error: PlaybackException) {
        val isNetworkError = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                             error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                             error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS

        if (isNetworkError && retryCount < maxRetries) {
            retryCount++
            _playbackState.value = _playbackState.value.copy(
                isReconnecting = true,
                reconnectAttempt = retryCount,
                errorMessage = null
            )
            reconnectJob?.cancel()
            reconnectJob = scope.launch {
                delay(2000L * retryCount)
                val current = currentMediaItemData
                if (current != null && exoPlayer != null) {
                    exoPlayer?.prepare()
                }
            }
        } else {
            val readableMessage = formatErrorMessage(error)
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                isBuffering = false,
                isReconnecting = false,
                errorMessage = readableMessage
            )
            _streamDiagnostics.value = _streamDiagnostics.value.copy(
                errorMessage = readableMessage
            )
        }
    }

    override fun release() {
        stopProgressTracker()
        reconnectJob?.cancel()
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun getPlayer(): Player? = exoPlayer

    private fun updatePlaybackState() {
        val player = exoPlayer ?: return
        val isBuffering = player.playbackState == Player.STATE_BUFFERING
        val isEnded = player.playbackState == Player.STATE_ENDED
        val duration = if (player.duration > 0 && player.duration != C.TIME_UNSET) player.duration else 0L
        val isLive = player.isCurrentMediaItemLive
        val isSeekableLive = isLive && player.isCurrentMediaItemSeekable
        val buffered = player.bufferedPosition.coerceAtLeast(0L)
        val current = player.currentPosition.coerceAtLeast(0L)
        val bufferedAhead = (buffered - current).coerceAtLeast(0L)

        _playbackState.value = _playbackState.value.copy(
            isPlaying = player.isPlaying,
            isBuffering = isBuffering,
            isEnded = isEnded,
            currentPositionMs = current,
            durationMs = duration,
            bufferedPositionMs = buffered,
            playbackSpeed = player.playbackParameters.speed,
            isLive = isLive,
            isSeekableLive = isSeekableLive,
            liveWindowMs = if (isSeekableLive) duration else 0L,
            errorMessage = if (player.playerError == null) null else _playbackState.value.errorMessage
        )

        _streamDiagnostics.value = _streamDiagnostics.value.copy(
            durationMs = if (duration > 0) duration else null,
            bufferedAheadMs = bufferedAhead,
            isLive = isLive
        )
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive) {
                updatePlaybackState()
                delay(400)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updateDiagnosticsFromTracks(tracks: Tracks) {
        var videoCodec: String? = null
        var audioCodec: String? = null
        var bitrate: Long? = null
        var frameRate: Float? = null
        var videoCount = 0
        var audioCount = 0
        var textCount = 0

        for (i in 0 until tracks.groups.size) {
            val group = tracks.groups[i]
            when (group.type) {
                C.TRACK_TYPE_VIDEO -> {
                    videoCount += group.length
                    for (j in 0 until group.length) {
                        if (group.isTrackSelected(j)) {
                            val format = group.mediaTrackGroup.getFormat(j)
                            videoCodec = format.sampleMimeType ?: format.codecs
                            if (format.bitrate != androidx.media3.common.Format.NO_VALUE) bitrate = format.bitrate.toLong()
                            if (format.frameRate > 0) frameRate = format.frameRate
                        }
                    }
                }
                C.TRACK_TYPE_AUDIO -> {
                    audioCount += group.length
                    for (j in 0 until group.length) {
                        if (group.isTrackSelected(j)) {
                            val format = group.mediaTrackGroup.getFormat(j)
                            audioCodec = format.sampleMimeType ?: format.codecs
                        }
                    }
                }
                C.TRACK_TYPE_TEXT -> {
                    textCount += group.length
                }
            }
        }

        val videoDecoder = videoCodec?.let { DecoderCapabilityDetector.findBestDecoderForMime(it) }
        val audioDecoder = audioCodec?.let { DecoderCapabilityDetector.findBestDecoderForMime(it) }

        _streamDiagnostics.value = _streamDiagnostics.value.copy(
            videoTracksCount = videoCount,
            audioTracksCount = audioCount,
            subtitleTracksCount = textCount + dynamicallyAddedSubtitles.size,
            videoCodec = videoCodec,
            videoDecoderName = videoDecoder?.name ?: "Android MediaCodec",
            isHardwareVideoDecoder = videoDecoder?.isHardware ?: true,
            audioCodec = audioCodec,
            audioDecoderName = audioDecoder?.name ?: "Android AudioDecoder",
            isHardwareAudioDecoder = audioDecoder?.isHardware ?: true,
            bitrate = bitrate,
            frameRate = frameRate
        )
    }

    private fun formatErrorMessage(error: PlaybackException): String {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                "The server returned an HTTP error (${error.message}). The URL may require authorization or has expired."
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "Connection failed. Please check your internet connection or stream server."
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                "Invalid or corrupt media stream. The manifest could not be parsed."
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED ->
                "This video or audio codec is not supported by your device decoders."
            else -> error.localizedMessage ?: "Playback failed. Please check the stream URL or retry."
        }
    }
}
