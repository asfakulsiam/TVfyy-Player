package com.example.domain.model

data class MediaItemData(
    val uri: String,
    val title: String = "Untitled Video",
    val streamType: StreamType = StreamType.UNKNOWN,
    val headers: Map<String, String> = emptyMap(),
    val subtitleUri: String? = null,
    val resumePositionMs: Long = 0L,
    val isLocalFile: Boolean = false,
    val mimeType: String? = null
)
