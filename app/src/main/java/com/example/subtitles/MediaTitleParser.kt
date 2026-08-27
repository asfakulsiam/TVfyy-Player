package com.example.subtitles

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class ParsedMediaMetadata(
    val cleanTitle: String,
    val year: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val rawName: String
)

object MediaTitleParser {

    private val RELEASE_NOISE_REGEX = Regex(
        "(?i)\\b(1080p|720p|480p|2160p|4k|uhd|bluray|blu-ray|bdrip|brrip|dvdrip|web-dl|webrip|web|hdtv|x264|x265|hevc|avc|10bit|aac|ac3|eac3|dts|dts-hd|truehd|atmos|repack|proper|unrated|extended|remux|hdr|sdr|yify|yts|rarbg|psa|eztv|tgx|vostfr)\\b"
    )

    private val SEASON_EPISODE_REGEX = Regex("(?i)s(\\d{1,2})e(\\d{1,2})")
    private val YEAR_REGEX = Regex("\\b(19\\d{2}|20\\d{2})\\b")

    fun parse(rawInput: String): ParsedMediaMetadata {
        var rawName = rawInput.substringBefore("?")
        // If it's a URL or file path, extract the last segment
        if (rawName.contains("/") || rawName.contains("\\")) {
            val lastSegment = rawName.substringAfterLast("/").substringAfterLast("\\")
            if (lastSegment.isNotBlank()) {
                rawName = try {
                    URLDecoder.decode(lastSegment, StandardCharsets.UTF_8.name())
                } catch (_: Exception) {
                    lastSegment
                }
            }
        }

        // Strip file extension
        val withoutExt = rawName.replace(Regex("\\.(mp4|mkv|avi|mov|ts|m3u8|mpd|webm|flv|wmv)$", RegexOption.IGNORE_CASE), "")

        // Clean dots, underscores, dashes, brackets into spaces first
        var working = withoutExt
            .replace(Regex("\\[.*?\\]"), " ")
            .replace(Regex("\\(.*?\\)"), " ")
            .replace('.', ' ')
            .replace('_', ' ')
            .replace('-', ' ')

        // Extract Season and Episode
        var season: Int? = null
        var episode: Int? = null
        val seMatch = SEASON_EPISODE_REGEX.find(working)
        if (seMatch != null) {
            season = seMatch.groupValues[1].toIntOrNull()
            episode = seMatch.groupValues[2].toIntOrNull()
        }

        // Extract Year
        var year: String? = null
        val yearMatch = YEAR_REGEX.find(working)
        if (yearMatch != null) {
            year = yearMatch.groupValues[1]
        }

        // Strip noise tags
        working = RELEASE_NOISE_REGEX.replace(working, " ")

        // If season/episode or year was present, cut off title before that point
        if (seMatch != null) {
            val idx = working.indexOf(seMatch.value, ignoreCase = true)
            if (idx > 1) {
                working = working.substring(0, idx)
            }
        } else if (yearMatch != null) {
            val idx = working.indexOf(yearMatch.value)
            if (idx > 1) {
                working = working.substring(0, idx)
            }
        }

        val cleaned = working
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { rawName }

        return ParsedMediaMetadata(
            cleanTitle = capitalizeWords(cleaned),
            year = year,
            season = season,
            episode = episode,
            rawName = rawName
        )
    }

    private fun capitalizeWords(input: String): String {
        return input.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
}
