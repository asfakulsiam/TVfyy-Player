package com.example.domain.model

enum class BufferPreset(val displayName: String, val description: String, val minBufferMs: Int, val maxBufferMs: Int) {
    LOW_LATENCY("Low Latency", "5s min / 15s max buffer - Ideal for real-time live sports", 5000, 15000),
    BALANCED("Balanced (Recommended)", "20s min / 50s max buffer - Smooth seeking & stability", 20000, 50000),
    AGGRESSIVE("Aggressive", "45s min / 90s max buffer - Best for unstable mobile connections", 45000, 90000),
    HUGE("Huge Cache", "90s min / 180s max buffer - Maximum offline caching & buffer depth", 90000, 180000)
}

enum class SubtitleAutoSelectMode(val displayName: String) {
    OFF("Always Off"),
    AUTO_MATCH("Auto Match Preferred Language"),
    ALWAYS_ON("Always On (First Available)")
}

data class PlayerSettings(
    val doubleTapSeekSeconds: Int = 10,
    val bufferPreset: BufferPreset = BufferPreset.BALANCED,
    val autoPlayNext: Boolean = true,
    val backgroundPlayback: Boolean = true,
    val hardwareAcceleration: Boolean = true,
    val defaultSpeed: Float = 1.0f,
    val defaultResizeMode: String = "FIT",
    val keepScreenAwake: Boolean = true,
    val gestureControlsEnabled: Boolean = true,
    val preferredAudioLanguage: String = "en",
    val preferredSubtitleLanguage: String = "en",
    val subtitleAutoSelectMode: SubtitleAutoSelectMode = SubtitleAutoSelectMode.AUTO_MATCH,
    val subtitleStyle: SubtitleStyleConfig = SubtitleStyleConfig(),
    val showQualityControl: Boolean = true,
    val showAudioControl: Boolean = true,
    val showSubtitleControl: Boolean = true,
    val showSpeedControl: Boolean = true,
    val showLockControl: Boolean = true,
    val showPipControl: Boolean = true,
    val showDiagnosticsControl: Boolean = true,
    val showResizeControl: Boolean = true
)
