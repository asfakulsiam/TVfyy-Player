package com.example.domain.model

enum class StreamType(val displayName: String) {
    PROGRESSIVE("Direct Media (MP4/WebM/MKV)"),
    HLS("HTTP Live Streaming (HLS)"),
    DASH("Dynamic Adaptive Streaming over HTTP (DASH)"),
    MPEG_TS("MPEG-TS Stream"),
    UNKNOWN("Auto-Detect Stream")
}
