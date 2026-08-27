package com.example.data.parser

import com.example.domain.model.Playlist
import com.example.domain.model.PlaylistChannel
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

data class M3uExportOptions(
    val includeTvgMetadata: Boolean = true,
    val includeLogos: Boolean = true,
    val includeCategories: Boolean = true,
    val includeCustomAttributes: Boolean = true
)

object M3uExporter {

    fun exportToString(
        playlist: Playlist?,
        channels: List<PlaylistChannel>,
        options: M3uExportOptions = M3uExportOptions()
    ): String {
        val sb = StringBuilder()
        sb.append("#EXTM3U\n")

        if (playlist != null && playlist.name.isNotBlank()) {
            sb.append("#PLAYLIST:").append(escapeHeaderValue(playlist.name)).append("\n")
        }

        for (channel in channels) {
            val extinf = buildExtinfLine(channel, options)
            sb.append(extinf).append("\n")
            sb.append(channel.streamUrl.trim()).append("\n")
        }

        return sb.toString()
    }

    fun exportToStream(
        outputStream: OutputStream,
        playlist: Playlist?,
        channels: List<PlaylistChannel>,
        options: M3uExportOptions = M3uExportOptions()
    ) {
        val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        writer.write(exportToString(playlist, channels, options))
        writer.flush()
    }

    private fun buildExtinfLine(channel: PlaylistChannel, options: M3uExportOptions): String {
        val sb = StringBuilder()
        sb.append("#EXTINF:-1")

        // 1. TVG ID
        if (options.includeTvgMetadata && !channel.tvgId.isNullOrBlank()) {
            sb.append(" tvg-id=\"").append(escapeAttributeValue(channel.tvgId)).append("\"")
        }

        // 2. TVG Name
        if (options.includeTvgMetadata && !channel.tvgName.isNullOrBlank()) {
            sb.append(" tvg-name=\"").append(escapeAttributeValue(channel.tvgName)).append("\"")
        }

        // 3. TVG Logo
        if (options.includeLogos && !channel.tvgLogo.isNullOrBlank()) {
            sb.append(" tvg-logo=\"").append(escapeAttributeValue(channel.tvgLogo)).append("\"")
        }

        // 4. Group Title / Category
        if (options.includeCategories && channel.categoryName.isNotBlank() && channel.categoryName != "Uncategorized") {
            sb.append(" group-title=\"").append(escapeAttributeValue(channel.categoryName)).append("\"")
        }

        // 5. Known attributes (like radio, catchup, etc.)
        if (options.includeTvgMetadata && channel.knownAttributes.isNotEmpty()) {
            for ((key, value) in channel.knownAttributes) {
                if (key != "tvg-id" && key != "tvg-name" && key != "tvg-logo" && key != "group-title") {
                    if (value.isNotBlank()) {
                        sb.append(" ").append(key).append("=\"").append(escapeAttributeValue(value)).append("\"")
                    }
                }
            }
        }

        // 6. Unknown / Custom attributes preserved from original import
        if (options.includeCustomAttributes && channel.unknownAttributes.isNotEmpty()) {
            for ((key, value) in channel.unknownAttributes) {
                if (key.isNotBlank()) {
                    sb.append(" ").append(key).append("=\"").append(escapeAttributeValue(value)).append("\"")
                }
            }
        }

        // 7. Comma and Display Channel Name
        sb.append(",").append(channel.name.trim())

        return sb.toString()
    }

    private fun escapeAttributeValue(value: String): String {
        return value.replace("\"", "\\\"").replace("\n", " ").replace("\r", "")
    }

    private fun escapeHeaderValue(value: String): String {
        return value.replace("\n", " ").replace("\r", "")
    }
}
