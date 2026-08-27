package com.example.domain.model

enum class SubtitleSource {
    EMBEDDED,
    EXTERNAL_FILE,
    EXTERNAL_URL,
    ONLINE_DOWNLOAD
}

enum class SubtitleFontSize(val displayName: String, val sp: Float) {
    SMALL("Small (14sp)", 14f),
    NORMAL("Normal (18sp)", 18f),
    LARGE("Large (24sp)", 24f),
    EXTRA_LARGE("Extra Large (30sp)", 30f)
}

enum class SubtitlePosition(val displayName: String, val verticalPercent: Float) {
    BOTTOM("Bottom", 0.88f),
    CENTER("Center", 0.50f),
    TOP("Top", 0.12f)
}

data class SubtitleStyleConfig(
    val fontSize: SubtitleFontSize = SubtitleFontSize.NORMAL,
    val textColorHex: Long = 0xFFFFFFFF,
    val backgroundColorHex: Long = 0x80000000,
    val outlineColorHex: Long = 0xFF000000,
    val outlineWidth: Float = 3f,
    val hasShadow: Boolean = true,
    val position: SubtitlePosition = SubtitlePosition.BOTTOM,
    val customVerticalOffsetPercent: Float = 0f, // -0.4f (higher) to +0.1f (lower)
    val customScaleFactor: Float = 1.0f,
    val encoding: String = "UTF-8"
) {
    val effectiveVerticalPercent: Float
        get() = (position.verticalPercent + customVerticalOffsetPercent).coerceIn(0.05f, 0.95f)

    val effectiveFontSizeSp: Float
        get() = (fontSize.sp * customScaleFactor).coerceIn(10f, 48f)
}

data class OnlineSubtitleItem(
    val id: String,
    val title: String,
    val language: String,
    val languageCode: String,
    val format: String, // SRT, VTT, ASS
    val downloadUrl: String,
    val releaseName: String,
    val downloadCount: Int = 0,
    val rating: Float = 0f,
    val provider: String = "OpenSubtitles",
    val fileSizeStr: String = "45 KB"
)

data class SubtitleSearchQuery(
    val title: String,
    val year: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val language: String = "all",
    val imdbId: String? = null
)

data class AudioChannelInfo(
    val channelCount: Int,
    val configuration: String
)
