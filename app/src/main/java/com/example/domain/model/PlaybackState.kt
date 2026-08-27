package com.example.domain.model

data class PlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val isLive: Boolean = false,
    val isSeekableLive: Boolean = false,
    val liveWindowMs: Long = 0L,
    val errorMessage: String? = null,
    val isMuted: Boolean = false,
    val volume: Float = 1.0f,
    val volumeBoostPercent: Int = 100, // 100, 125, 150
    val audioDelayMs: Long = 0L,
    val subtitleDelayMs: Long = 0L,
    val queueIndex: Int = 0,
    val queueTotal: Int = 1,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val zoomScale: Float = 1.0f,
    val zoomOffsetX: Float = 0f,
    val zoomOffsetY: Float = 0f,
    val isReconnecting: Boolean = false,
    val reconnectAttempt: Int = 0
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val bufferedProgress: Float
        get() = if (durationMs > 0) (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val isBehindLiveEdge: Boolean
        get() = isLive && isSeekableLive && (durationMs - currentPositionMs > 15_000L)
}

