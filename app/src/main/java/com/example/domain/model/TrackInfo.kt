package com.example.domain.model

enum class TrackType {
    VIDEO, AUDIO, SUBTITLE
}

data class TrackInfo(
    val id: String,
    val trackGroupIndex: Int,
    val trackIndex: Int,
    val type: TrackType,
    val label: String,
    val language: String? = null,
    val languageCode: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val bitrate: Long = 0L,
    val frameRate: Float = 0f,
    val mimeType: String? = null,
    val codecs: String? = null,
    val channelCount: Int = 0,
    val channelConfiguration: String? = null,
    val roleFlags: Int = 0,
    val isDefault: Boolean = false,
    val isForced: Boolean = false,
    val isDub: Boolean = false,
    val isCommentary: Boolean = false,
    val isOriginal: Boolean = false,
    val isSDH: Boolean = false,
    val source: SubtitleSource = SubtitleSource.EMBEDDED,
    val formatName: String? = null,
    val filePath: String? = null,
    val encoding: String? = null,
    val isSelected: Boolean = false
) {
    val resolutionLabel: String
        get() = if (width > 0 && height > 0) "${width}x${height} (${height}p)" else label

    val audioDetailsLabel: String
        get() {
            val parts = mutableListOf<String>()
            if (!codecs.isNullOrBlank()) parts.add(codecs)
            else if (!mimeType.isNullOrBlank()) {
                val shortMime = mimeType.substringAfterLast("/")
                parts.add(shortMime.uppercase())
            }
            if (!channelConfiguration.isNullOrBlank()) parts.add(channelConfiguration)
            if (bitrate > 0) parts.add("${bitrate / 1000} kbps")
            return parts.joinToString(" • ")
        }
}
