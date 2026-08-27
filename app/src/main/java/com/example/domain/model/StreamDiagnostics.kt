package com.example.domain.model

data class StreamDiagnostics(
    val url: String = "",
    val reachable: Boolean = false,
    val httpStatusCode: Int? = null,
    val httpStatusMessage: String? = null,
    val contentType: String? = null,
    val redirectsCount: Int = 0,
    val finalUrl: String? = null,
    val detectedStreamType: StreamType = StreamType.UNKNOWN,
    val videoTracksCount: Int = 0,
    val audioTracksCount: Int = 0,
    val subtitleTracksCount: Int = 0,
    val resolution: String? = null,
    val videoCodec: String? = null,
    val videoDecoderName: String? = null,
    val isHardwareVideoDecoder: Boolean? = null,
    val audioCodec: String? = null,
    val audioDecoderName: String? = null,
    val isHardwareAudioDecoder: Boolean? = null,
    val bitrate: Long? = null,
    val frameRate: Float? = null,
    val durationMs: Long? = null,
    val bufferedAheadMs: Long? = null,
    val estimatedBandwidthKbps: Long? = null,
    val isLive: Boolean = false,
    val headersSent: Map<String, String> = emptyMap(),
    val errorMessage: String? = null
) {
    val maskedUrl: String
        get() {
            if (url.isBlank()) return ""
            // Mask query parameters containing token, auth, secret, key, signature, pass
            return url.replace(Regex("(?i)(token|auth|key|secret|signature|pass|password|user)=([^&]+)")) {
                "${it.groupValues[1]}=********"
            }
        }
}

