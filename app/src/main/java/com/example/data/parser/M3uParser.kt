package com.example.data.parser

import com.example.domain.model.ParsedEntry
import com.example.domain.model.ParsedPlaylist
import com.example.domain.model.ParserWarning
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.util.regex.Pattern

object M3uParser {

    private val KNOWN_ATTRIBUTE_KEYS = setOf(
        "tvg-id",
        "tvg-name",
        "tvg-logo",
        "group-title",
        "radio",
        "tvg-shift",
        "tvg-chno",
        "tvg-country",
        "tvg-language",
        "catchup",
        "catchup-source",
        "catchup-days",
        "timeshift",
        "aspect-ratio",
        "audio-track",
        "user-agent",
        "http-user-agent",
        "http-referrer"
    )

    // Regex to match key="value" or key=value attributes in #EXTINF tag
    private val ATTRIBUTE_PATTERN = Pattern.compile("([a-zA-Z0-9_-]+)\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s,]+))")

    fun parse(content: String, defaultName: String? = null): ParsedPlaylist {
        return parseReader(BufferedReader(StringReader(content)), defaultName)
    }

    fun parse(inputStream: InputStream, defaultName: String? = null): ParsedPlaylist {
        return parseReader(BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)), defaultName)
    }

    private fun parseReader(reader: BufferedReader, defaultName: String?): ParsedPlaylist {
        val entries = mutableListOf<ParsedEntry>()
        val warnings = mutableListOf<ParserWarning>()
        val categorySet = linkedSetOf<String>()

        var totalFound = 0
        var lineNumber = 0

        var pendingExtinfLine: String? = null
        var pendingExtinfLineNumber = 0
        var pendingExtgrp: String? = null

        reader.useLines { lines ->
            for (rawLine in lines) {
                lineNumber++
                val line = rawLine.trim()

                if (line.isEmpty()) {
                    continue
                }

                // Check for EXTM3U Header
                if (line.startsWith("#EXTM3U", ignoreCase = true)) {
                    continue
                }

                // Check for EXTM3U Group override tag
                if (line.startsWith("#EXTGRP:", ignoreCase = true)) {
                    pendingExtgrp = line.substringAfter(":").trim()
                    continue
                }

                // Other playlist comment / metadata tags that are not EXTINF
                if (line.startsWith("#") && !line.startsWith("#EXTINF:", ignoreCase = true)) {
                    continue
                }

                if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                    totalFound++
                    // If we already had a pending EXTINF without a stream URL, record warning
                    if (pendingExtinfLine != null) {
                        warnings.add(
                            ParserWarning(
                                lineNumber = pendingExtinfLineNumber,
                                rawContent = pendingExtinfLine,
                                reason = "Missing stream URL for channel entry"
                            )
                        )
                    }
                    pendingExtinfLine = line
                    pendingExtinfLineNumber = lineNumber
                    continue
                }

                // Found non-comment line while having a pending EXTINF
                if (pendingExtinfLine != null) {
                    val streamUrl = line
                    if (!isValidStreamUrl(streamUrl)) {
                        warnings.add(
                            ParserWarning(
                                lineNumber = lineNumber,
                                rawContent = streamUrl,
                                reason = "Invalid or empty media stream URL"
                            )
                        )
                    } else {
                        val parsed = parseExtinf(
                            extinfLine = pendingExtinfLine,
                            streamUrl = streamUrl,
                            extgrpOverride = pendingExtgrp,
                            lineNumber = pendingExtinfLineNumber
                        )

                        if (parsed != null) {
                            entries.add(parsed)
                            val category = parsed.groupTitle?.ifBlank { "Uncategorized" } ?: "Uncategorized"
                            categorySet.add(category)
                        } else {
                            warnings.add(
                                ParserWarning(
                                    lineNumber = pendingExtinfLineNumber,
                                    rawContent = pendingExtinfLine,
                                    reason = "Malformed EXTINF metadata line"
                                )
                            )
                        }
                    }

                    pendingExtinfLine = null
                    pendingExtinfLineNumber = 0
                    pendingExtgrp = null
                } else {
                    // Stray URL or line without #EXTINF
                    if (isValidStreamUrl(line)) {
                        totalFound++
                        val fallbackName = extractFallbackNameFromUrl(line)
                        entries.add(
                            ParsedEntry(
                                name = fallbackName,
                                streamUrl = line,
                                groupTitle = "Uncategorized",
                                rawLineNumber = lineNumber
                            )
                        )
                        categorySet.add("Uncategorized")
                    }
                }
            }
        }

        // Check if last line had pending EXTINF without URL
        if (pendingExtinfLine != null) {
            warnings.add(
                ParserWarning(
                    lineNumber = pendingExtinfLineNumber,
                    rawContent = pendingExtinfLine,
                    reason = "Playlist ended with an unclosed EXTINF tag without a stream URL"
                )
            )
        }

        return ParsedPlaylist(
            defaultName = defaultName,
            entries = entries,
            categories = categorySet.toList(),
            warnings = warnings,
            totalFound = totalFound,
            totalValid = entries.size
        )
    }

    private fun parseExtinf(
        extinfLine: String,
        streamUrl: String,
        extgrpOverride: String?,
        lineNumber: Int
    ): ParsedEntry? {
        try {
            val contentAfterPrefix = extinfLine.substringAfter(":", "").trim()
            if (contentAfterPrefix.isEmpty()) return null

            // Split into attributes portion and channel display name (after last comma or first unquoted comma)
            val commaIndex = findDisplayNameCommaIndex(contentAfterPrefix)
            val attributesPart: String
            val channelName: String

            if (commaIndex != -1) {
                attributesPart = contentAfterPrefix.substring(0, commaIndex).trim()
                channelName = contentAfterPrefix.substring(commaIndex + 1).trim()
            } else {
                attributesPart = contentAfterPrefix
                channelName = extractFallbackNameFromUrl(streamUrl)
            }

            // Extract duration from start of attributesPart e.g. "-1" or "0"
            val durationTokens = attributesPart.split(Pattern.compile("\\s+"), 2)
            val durationSeconds = durationTokens.firstOrNull()?.toIntOrNull() ?: -1
            val remainingAttributes = if (durationTokens.size > 1) durationTokens[1] else ""

            val knownAttributes = mutableMapOf<String, String>()
            val unknownAttributes = mutableMapOf<String, String>()

            if (remainingAttributes.isNotBlank()) {
                val matcher = ATTRIBUTE_PATTERN.matcher(remainingAttributes)
                while (matcher.find()) {
                    val key = matcher.group(1)?.lowercase() ?: continue
                    val value = matcher.group(3) ?: matcher.group(4) ?: matcher.group(5) ?: ""

                    if (KNOWN_ATTRIBUTE_KEYS.contains(key)) {
                        knownAttributes[key] = value
                    } else {
                        unknownAttributes[key] = value
                    }
                }
            }

            val tvgId = knownAttributes["tvg-id"]
            val tvgName = knownAttributes["tvg-name"]
            val tvgLogo = knownAttributes["tvg-logo"]
            val groupTitle = extgrpOverride ?: knownAttributes["group-title"] ?: "Uncategorized"

            val effectiveName = if (channelName.isNotBlank()) {
                channelName
            } else if (!tvgName.isNullOrBlank()) {
                tvgName
            } else {
                extractFallbackNameFromUrl(streamUrl)
            }

            return ParsedEntry(
                name = effectiveName,
                streamUrl = streamUrl,
                tvgId = tvgId,
                tvgName = tvgName,
                tvgLogo = tvgLogo,
                groupTitle = groupTitle.ifBlank { "Uncategorized" },
                durationSeconds = durationSeconds,
                knownAttributes = knownAttributes,
                unknownAttributes = unknownAttributes,
                rawLineNumber = lineNumber
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun findDisplayNameCommaIndex(text: String): Int {
        var inQuotes = false
        var quoteChar = '"'

        for (i in text.indices) {
            val c = text[i]
            if ((c == '"' || c == '\'') && (i == 0 || text[i - 1] != '\\')) {
                if (!inQuotes) {
                    inQuotes = true
                    quoteChar = c
                } else if (c == quoteChar) {
                    inQuotes = false
                }
            } else if (c == ',' && !inQuotes) {
                return i
            }
        }
        return text.lastIndexOf(',')
    }

    private fun isValidStreamUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        val lower = trimmed.lowercase()
        return lower.startsWith("http://") ||
                lower.startsWith("https://") ||
                lower.startsWith("rtmp://") ||
                lower.startsWith("rtsp://") ||
                lower.startsWith("content://") ||
                lower.startsWith("file://") ||
                lower.startsWith("/")
    }

    private fun extractFallbackNameFromUrl(url: String): String {
        return try {
            val clean = url.substringBefore("?").substringBefore("#")
            val segment = clean.substringAfterLast("/").substringBeforeLast(".")
            if (segment.isNotBlank()) {
                segment.replace("-", " ").replace("_", " ").trim().capitalizeWords()
            } else {
                "Channel"
            }
        } catch (_: Exception) {
            "Channel"
        }
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
