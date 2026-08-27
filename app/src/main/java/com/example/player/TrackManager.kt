package com.example.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.domain.model.SubtitleSource
import com.example.domain.model.TrackInfo
import com.example.domain.model.TrackType
import java.util.Locale

class TrackManager(private val trackSelector: DefaultTrackSelector) {

    fun extractVideoTracks(tracks: Tracks): List<TrackInfo> {
        val list = mutableListOf<TrackInfo>()
        for (i in 0 until tracks.groups.size) {
            val groupInfo = tracks.groups[i]
            if (groupInfo.type == C.TRACK_TYPE_VIDEO) {
                val group = groupInfo.mediaTrackGroup
                for (j in 0 until group.length) {
                    val format = group.getFormat(j)
                    val label = buildVideoLabel(format)
                    val isSelected = groupInfo.isTrackSelected(j)
                    list.add(
                        TrackInfo(
                            id = format.id ?: "video_${i}_$j",
                            trackGroupIndex = i,
                            trackIndex = j,
                            type = TrackType.VIDEO,
                            label = label,
                            width = format.width,
                            height = format.height,
                            bitrate = if (format.bitrate != Format.NO_VALUE) format.bitrate.toLong() else 0L,
                            frameRate = if (format.frameRate != Format.NO_VALUE.toFloat()) format.frameRate else 0f,
                            mimeType = format.sampleMimeType,
                            codecs = format.codecs,
                            isSelected = isSelected
                        )
                    )
                }
            }
        }
        return list.sortedByDescending { it.height * 100000 + it.bitrate }
    }

    fun extractAudioTracks(tracks: Tracks): List<TrackInfo> {
        val list = mutableListOf<TrackInfo>()
        for (i in 0 until tracks.groups.size) {
            val groupInfo = tracks.groups[i]
            if (groupInfo.type == C.TRACK_TYPE_AUDIO) {
                val group = groupInfo.mediaTrackGroup
                for (j in 0 until group.length) {
                    val format = group.getFormat(j)
                    val langCode = format.language?.lowercase()
                    val langDisplay = resolveLanguageDisplayName(langCode)
                    val channelConfig = formatChannelConfiguration(format.channelCount)

                    val isDub = (format.roleFlags and C.ROLE_FLAG_DUB) != 0 || (format.label?.contains("dub", ignoreCase = true) == true)
                    val isCommentary = (format.roleFlags and C.ROLE_FLAG_COMMENTARY) != 0 || (format.label?.contains("commentary", ignoreCase = true) == true)
                    val isOriginal = (format.roleFlags and C.ROLE_FLAG_MAIN) != 0 || (format.label?.contains("original", ignoreCase = true) == true)
                    val isDefault = (format.selectionFlags and C.SELECTION_FLAG_DEFAULT) != 0

                    val codecName = formatAudioCodec(format.sampleMimeType, format.codecs)

                    val finalLabel = buildAudioLabel(
                        formatLabel = format.label,
                        langDisplay = langDisplay,
                        codecName = codecName,
                        channelConfig = channelConfig,
                        isDub = isDub,
                        isCommentary = isCommentary
                    )

                    val isSelected = groupInfo.isTrackSelected(j)

                    list.add(
                        TrackInfo(
                            id = format.id ?: "audio_${i}_$j",
                            trackGroupIndex = i,
                            trackIndex = j,
                            type = TrackType.AUDIO,
                            label = finalLabel,
                            language = langDisplay,
                            languageCode = langCode,
                            bitrate = if (format.bitrate != Format.NO_VALUE) format.bitrate.toLong() else 0L,
                            mimeType = format.sampleMimeType,
                            codecs = codecName,
                            channelCount = format.channelCount,
                            channelConfiguration = channelConfig,
                            roleFlags = format.roleFlags,
                            isDefault = isDefault,
                            isDub = isDub,
                            isCommentary = isCommentary,
                            isOriginal = isOriginal,
                            isSelected = isSelected
                        )
                    )
                }
            }
        }
        return list
    }

    fun extractSubtitleTracks(tracks: Tracks, externalTracks: List<TrackInfo> = emptyList()): List<TrackInfo> {
        val list = mutableListOf<TrackInfo>()
        for (i in 0 until tracks.groups.size) {
            val groupInfo = tracks.groups[i]
            if (groupInfo.type == C.TRACK_TYPE_TEXT) {
                val group = groupInfo.mediaTrackGroup
                for (j in 0 until group.length) {
                    val format = group.getFormat(j)
                    val langCode = format.language?.lowercase()
                    val langDisplay = resolveLanguageDisplayName(langCode)
                    val isSDH = (format.roleFlags and (C.ROLE_FLAG_TRANSCRIBES_DIALOG or C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND or C.ROLE_FLAG_DESCRIBES_VIDEO)) != 0
                    val isForced = (format.selectionFlags and C.SELECTION_FLAG_FORCED) != 0 || (format.roleFlags and C.ROLE_FLAG_SUPPLEMENTARY) != 0
                    val isDefault = (format.selectionFlags and C.SELECTION_FLAG_DEFAULT) != 0

                    val formatName = when {
                        format.sampleMimeType?.contains("vtt", ignoreCase = true) == true -> "WebVTT"
                        format.sampleMimeType?.contains("subrip", ignoreCase = true) == true -> "SRT"
                        format.sampleMimeType?.contains("ssa", ignoreCase = true) == true -> "ASS"
                        format.sampleMimeType?.contains("ttml", ignoreCase = true) == true -> "TTML"
                        else -> "Subtitle"
                    }

                    val finalLabel = buildSubtitleLabel(
                        formatLabel = format.label,
                        langDisplay = langDisplay,
                        isSDH = isSDH,
                        isForced = isForced,
                        formatName = formatName
                    )

                    val isSelected = groupInfo.isTrackSelected(j)

                    list.add(
                        TrackInfo(
                            id = format.id ?: "text_${i}_$j",
                            trackGroupIndex = i,
                            trackIndex = j,
                            type = TrackType.SUBTITLE,
                            label = finalLabel,
                            language = langDisplay,
                            languageCode = langCode,
                            mimeType = format.sampleMimeType,
                            source = SubtitleSource.EMBEDDED,
                            formatName = formatName,
                            roleFlags = format.roleFlags,
                            isDefault = isDefault,
                            isForced = isForced,
                            isSDH = isSDH,
                            isSelected = isSelected
                        )
                    )
                }
            }
        }

        // Include any dynamically loaded external or online subtitle files
        list.addAll(externalTracks)
        return list
    }

    fun selectVideoTrack(tracks: Tracks, trackInfo: TrackInfo?) {
        val builder = trackSelector.buildUponParameters()
        if (trackInfo == null) {
            builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            builder.setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
        } else {
            for (i in 0 until tracks.groups.size) {
                val groupInfo = tracks.groups[i]
                if (groupInfo.type == C.TRACK_TYPE_VIDEO && i == trackInfo.trackGroupIndex) {
                    val override = TrackSelectionOverride(groupInfo.mediaTrackGroup, trackInfo.trackIndex)
                    builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                    builder.addOverride(override)
                    break
                }
            }
        }
        trackSelector.setParameters(builder)
    }

    fun selectAudioTrack(tracks: Tracks, trackInfo: TrackInfo) {
        val builder = trackSelector.buildUponParameters()
        for (i in 0 until tracks.groups.size) {
            val groupInfo = tracks.groups[i]
            if (groupInfo.type == C.TRACK_TYPE_AUDIO && i == trackInfo.trackGroupIndex) {
                val override = TrackSelectionOverride(groupInfo.mediaTrackGroup, trackInfo.trackIndex)
                builder.clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                builder.addOverride(override)
                break
            }
        }
        trackSelector.setParameters(builder)
    }

    fun selectSubtitleTrack(tracks: Tracks, trackInfo: TrackInfo?) {
        val builder = trackSelector.buildUponParameters()
        if (trackInfo == null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            builder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
        } else {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            if (trackInfo.trackGroupIndex >= 0) {
                for (i in 0 until tracks.groups.size) {
                    val groupInfo = tracks.groups[i]
                    if (groupInfo.type == C.TRACK_TYPE_TEXT && i == trackInfo.trackGroupIndex) {
                        val override = TrackSelectionOverride(groupInfo.mediaTrackGroup, trackInfo.trackIndex)
                        builder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        builder.addOverride(override)
                        break
                    }
                }
            }
        }
        trackSelector.setParameters(builder)
    }

    fun applyPreferredLanguages(preferredAudioLang: String?, preferredSubtitleLang: String?) {
        val builder = trackSelector.buildUponParameters()
        if (!preferredAudioLang.isNullOrBlank()) {
            builder.setPreferredAudioLanguage(preferredAudioLang)
        }
        if (!preferredSubtitleLang.isNullOrBlank()) {
            builder.setPreferredTextLanguage(preferredSubtitleLang)
        }
        trackSelector.setParameters(builder)
    }

    private fun resolveLanguageDisplayName(code: String?): String {
        if (code.isNullOrBlank()) return "Original / Undetermined"
        return when (code.lowercase()) {
            "bn", "ben" -> "Bangla (Bengali)"
            "hi", "hin" -> "Hindi"
            "en", "eng" -> "English"
            "es", "spa" -> "Spanish"
            "ar", "ara" -> "Arabic"
            "ja", "jpn" -> "Japanese"
            "fr", "fra", "fre" -> "French"
            "de", "deu", "ger" -> "German"
            "pt", "por" -> "Portuguese"
            "ko", "kor" -> "Korean"
            "ru", "rus" -> "Russian"
            "it", "ita" -> "Italian"
            "zh", "zho", "chi" -> "Chinese"
            "tr", "tur" -> "Turkish"
            "ur", "urd" -> "Urdu"
            "fa", "fas", "per" -> "Persian"
            "ta", "tam" -> "Tamil"
            "te", "tel" -> "Telugu"
            "ml", "mal" -> "Malayalam"
            else -> {
                try {
                    Locale.forLanguageTag(code).displayLanguage.ifBlank { code.uppercase() }
                } catch (_: Exception) {
                    code.uppercase()
                }
            }
        }
    }

    private fun formatChannelConfiguration(channels: Int): String {
        return when (channels) {
            1 -> "Mono"
            2 -> "Stereo (2.0)"
            6 -> "5.1 Surround"
            8 -> "7.1 Surround"
            else -> if (channels > 0) "$channels Ch" else "Stereo"
        }
    }

    private fun formatAudioCodec(mimeType: String?, codecs: String?): String {
        return when {
            codecs?.contains("ec-3", ignoreCase = true) == true || mimeType?.contains("eac3", ignoreCase = true) == true -> "Dolby Digital Plus (E-AC-3)"
            codecs?.contains("ac-3", ignoreCase = true) == true || mimeType?.contains("ac3", ignoreCase = true) == true -> "Dolby Digital (AC-3)"
            codecs?.contains("dts", ignoreCase = true) == true || mimeType?.contains("dts", ignoreCase = true) == true -> "DTS Digital"
            codecs?.contains("opus", ignoreCase = true) == true || mimeType?.contains("opus", ignoreCase = true) == true -> "Opus"
            codecs?.contains("flac", ignoreCase = true) == true || mimeType?.contains("flac", ignoreCase = true) == true -> "FLAC"
            codecs?.contains("mp4a", ignoreCase = true) == true || mimeType?.contains("aac", ignoreCase = true) == true -> "AAC"
            codecs?.contains("mp3", ignoreCase = true) == true || mimeType?.contains("mpeg", ignoreCase = true) == true -> "MP3"
            !codecs.isNullOrBlank() -> codecs
            !mimeType.isNullOrBlank() -> mimeType.substringAfterLast("/").uppercase()
            else -> "AAC"
        }
    }

    private fun buildAudioLabel(
        formatLabel: String?,
        langDisplay: String,
        codecName: String,
        channelConfig: String,
        isDub: Boolean,
        isCommentary: Boolean
    ): String {
        if (!formatLabel.isNullOrBlank()) {
            return formatLabel
        }
        val prefix = when {
            isDub -> "$langDisplay [Dub]"
            isCommentary -> "$langDisplay [Commentary]"
            else -> langDisplay
        }
        return "$prefix • $codecName ($channelConfig)"
    }

    private fun buildSubtitleLabel(
        formatLabel: String?,
        langDisplay: String,
        isSDH: Boolean,
        isForced: Boolean,
        formatName: String
    ): String {
        if (!formatLabel.isNullOrBlank()) return formatLabel
        val tags = mutableListOf<String>()
        if (isSDH) tags.add("SDH")
        if (isForced) tags.add("Forced")
        val suffix = if (tags.isNotEmpty()) " [${tags.joinToString(", ")}]" else ""
        return "$langDisplay$suffix ($formatName)"
    }

    private fun buildVideoLabel(format: Format): String {
        return when {
            format.height > 0 -> {
                val p = "${format.height}p"
                val fps = if (format.frameRate > 0) " ${format.frameRate.toInt()}fps" else ""
                val mbps = if (format.bitrate > 0) " (${String.format(Locale.US, "%.1f", format.bitrate / 1_000_000f)} Mbps)" else ""
                "$p$fps$mbps"
            }
            format.bitrate > 0 -> "${format.bitrate / 1000} kbps"
            !format.label.isNullOrBlank() -> format.label!!
            else -> "Quality Track"
        }
    }
}
