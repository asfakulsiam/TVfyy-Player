package com.example.resolver

import android.net.Uri
import com.example.domain.model.StreamType
import java.util.Locale

object MediaTypeDetector {

    fun detectFromExtension(url: String): StreamType {
        val cleanUrl = url.trim().lowercase(Locale.ROOT)
        val path = try {
            val uri = Uri.parse(cleanUrl)
            uri.path ?: cleanUrl
        } catch (_: Exception) {
            cleanUrl
        }

        return when {
            path.endsWith(".m3u8") || path.contains(".m3u8?") || cleanUrl.contains("/hls/") -> StreamType.HLS
            path.endsWith(".mpd") || path.contains(".mpd?") || cleanUrl.contains("/dash/") -> StreamType.DASH
            path.endsWith(".mp4") || path.endsWith(".m4v") || path.endsWith(".webm") ||
            path.endsWith(".mkv") || path.endsWith(".mov") || path.endsWith(".avi") ||
            path.endsWith(".flv") || path.endsWith(".ogv") || path.endsWith(".3gp") -> StreamType.PROGRESSIVE
            path.endsWith(".ts") -> StreamType.MPEG_TS
            else -> StreamType.UNKNOWN
        }
    }

    fun detectFromContentType(contentType: String?): StreamType {
        if (contentType.isNullOrBlank()) return StreamType.UNKNOWN
        val lower = contentType.lowercase(Locale.ROOT)
        return when {
            lower.contains("application/vnd.apple.mpegurl") ||
            lower.contains("application/x-mpegurl") ||
            lower.contains("vnd.apple.mpegurl") ||
            lower.contains("audio/mpegurl") ||
            lower.contains("audio/x-mpegurl") -> StreamType.HLS

            lower.contains("application/dash+xml") ||
            lower.contains("video/vnd.mpeg.dash.mpd") -> StreamType.DASH

            lower.contains("video/mp4") ||
            lower.contains("video/webm") ||
            lower.contains("video/x-matroska") ||
            lower.contains("video/quicktime") ||
            lower.contains("video/x-msvideo") ||
            lower.contains("video/ogg") ||
            lower.contains("application/mp4") ||
            lower.contains("application/octet-stream") -> StreamType.PROGRESSIVE

            lower.contains("video/mp2t") || lower.contains("video/ts") -> StreamType.MPEG_TS

            else -> StreamType.UNKNOWN
        }
    }
}
