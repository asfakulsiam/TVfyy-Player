package com.example.player

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build

data class CodecInfo(
    val name: String,
    val mimeType: String,
    val isHardware: Boolean,
    val isEncoder: Boolean,
    val maxSupportedInstances: Int
)

object DecoderCapabilityDetector {

    fun getSupportedVideoDecoders(): List<CodecInfo> {
        val results = mutableListOf<CodecInfo>()
        try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (codec in codecList.codecInfos) {
                if (codec.isEncoder) continue
                for (type in codec.supportedTypes) {
                    if (type.startsWith("video/")) {
                        results.add(
                            CodecInfo(
                                name = codec.name,
                                mimeType = type,
                                isHardware = isHardwareCodec(codec),
                                isEncoder = false,
                                maxSupportedInstances = try {
                                    codec.getCapabilitiesForType(type).maxSupportedInstances
                                } catch (_: Exception) { 1 }
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return results
    }

    fun getSupportedAudioDecoders(): List<CodecInfo> {
        val results = mutableListOf<CodecInfo>()
        try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (codec in codecList.codecInfos) {
                if (codec.isEncoder) continue
                for (type in codec.supportedTypes) {
                    if (type.startsWith("audio/")) {
                        results.add(
                            CodecInfo(
                                name = codec.name,
                                mimeType = type,
                                isHardware = isHardwareCodec(codec),
                                isEncoder = false,
                                maxSupportedInstances = try {
                                    codec.getCapabilitiesForType(type).maxSupportedInstances
                                } catch (_: Exception) { 1 }
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return results
    }

    fun isHardwareCodec(codecInfo: MediaCodecInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            codecInfo.isHardwareAccelerated
        } else {
            val name = codecInfo.name.lowercase()
            !name.startsWith("omx.google.") &&
            !name.startsWith("c2.android.") &&
            !name.contains("sw") &&
            !name.contains("soft")
        }
    }

    fun findBestDecoderForMime(mimeType: String): CodecInfo? {
        val decoders = if (mimeType.startsWith("video/")) getSupportedVideoDecoders() else getSupportedAudioDecoders()
        val matching = decoders.filter { it.mimeType.equals(mimeType, ignoreCase = true) }
        return matching.firstOrNull { it.isHardware } ?: matching.firstOrNull()
    }
}
