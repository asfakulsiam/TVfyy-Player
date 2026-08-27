package com.example.subtitles

import android.content.Context
import android.net.Uri
import androidx.media3.common.MimeTypes
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object SubtitleFileValidator {

    private val SUPPORTED_EXTENSIONS = listOf("srt", "vtt", "ass", "ssa", "ttml")

    data class ValidationResult(
        val isValid: Boolean,
        val detectedFormat: String? = null,
        val mimeType: String? = null,
        val detectedEncoding: String = "UTF-8",
        val errorMessage: String? = null,
        val normalizedFile: File? = null
    )

    fun detectMimeType(fileNameOrUri: String): String {
        val lower = fileNameOrUri.lowercase()
        return when {
            lower.endsWith(".vtt") -> MimeTypes.TEXT_VTT
            lower.endsWith(".srt") -> MimeTypes.APPLICATION_SUBRIP
            lower.endsWith(".ass") || lower.endsWith(".ssa") -> MimeTypes.TEXT_SSA
            lower.endsWith(".ttml") || lower.endsWith(".xml") -> MimeTypes.APPLICATION_TTML
            else -> MimeTypes.TEXT_VTT
        }
    }

    fun detectFormatName(mimeTypeOrExt: String): String {
        val lower = mimeTypeOrExt.lowercase()
        return when {
            lower.contains("vtt") -> "WebVTT"
            lower.contains("subrip") || lower.contains("srt") -> "SubRip (SRT)"
            lower.contains("ssa") || lower.contains("ass") -> "SubStation Alpha (ASS)"
            lower.contains("ttml") -> "TTML"
            else -> "Subtitle"
        }
    }

    /**
     * Reads subtitle bytes from Uri, detects encoding, converts to clean UTF-8,
     * and saves to a safe validated cache file.
     */
    fun validateAndNormalizeSubtitle(
        context: Context,
        uri: Uri,
        outputDir: File,
        preferredEncoding: String? = null
    ): ValidationResult {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(uri)
                ?: return ValidationResult(isValid = false, errorMessage = "Could not open subtitle source.")

            val rawBytes = inputStream.use { it.readBytes() }
            if (rawBytes.isEmpty()) {
                return ValidationResult(isValid = false, errorMessage = "Subtitle file is empty.")
            }

            // Quick security validation: Check for binary executable signatures (e.g. ELF, MZ, DEX, ZIP)
            if (isBinaryExecutable(rawBytes)) {
                return ValidationResult(isValid = false, errorMessage = "File is not a valid subtitle text format.")
            }

            val charset = if (!preferredEncoding.isNullOrBlank() && preferredEncoding != "AUTO") {
                try {
                    Charset.forName(preferredEncoding)
                } catch (_: Exception) {
                    detectCharset(rawBytes)
                }
            } else {
                detectCharset(rawBytes)
            }

            val textContent = String(rawBytes, charset)
            val format = detectFormatFromContent(textContent, uri.toString())

            if (format == null) {
                return ValidationResult(
                    isValid = false,
                    errorMessage = "Unsupported subtitle structure. File must be SRT, WebVTT, or ASS/SSA."
                )
            }

            // Normalize text to UTF-8
            val safeFileName = "sub_${System.currentTimeMillis()}.${format.lowercase()}"
            if (!outputDir.exists()) outputDir.mkdirs()
            val outputFile = File(outputDir, safeFileName)
            outputFile.writeText(textContent, StandardCharsets.UTF_8)

            val mime = when (format) {
                "VTT" -> MimeTypes.TEXT_VTT
                "SRT" -> MimeTypes.APPLICATION_SUBRIP
                "ASS" -> MimeTypes.TEXT_SSA
                else -> MimeTypes.TEXT_VTT
            }

            ValidationResult(
                isValid = true,
                detectedFormat = format,
                mimeType = mime,
                detectedEncoding = charset.name(),
                normalizedFile = outputFile
            )
        } catch (e: Exception) {
            ValidationResult(isValid = false, errorMessage = "Failed to process subtitle: ${e.localizedMessage}")
        }
    }

    private fun detectCharset(bytes: ByteArray): Charset {
        // Check Byte Order Mark (BOM)
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return StandardCharsets.UTF_8
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return StandardCharsets.UTF_16LE
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return StandardCharsets.UTF_16BE
        }

        // Try decoding as UTF-8 first
        return try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
            decoder.decode(java.nio.ByteBuffer.wrap(bytes))
            StandardCharsets.UTF_8
        } catch (_: Exception) {
            // Fallback to ISO-8859-1 (Latin-1) which accepts all byte values
            StandardCharsets.ISO_8859_1
        }
    }

    private fun detectFormatFromContent(content: String, pathHint: String): String? {
        val trimmed = content.trim()
        val pathLower = pathHint.lowercase()

        return when {
            trimmed.startsWith("WEBVTT", ignoreCase = true) || pathLower.endsWith(".vtt") -> "VTT"
            trimmed.contains("[Script Info]", ignoreCase = true) || pathLower.endsWith(".ass") || pathLower.endsWith(".ssa") -> "ASS"
            trimmed.contains("-->") || pathLower.endsWith(".srt") -> "SRT"
            trimmed.startsWith("<?xml", ignoreCase = true) || pathLower.endsWith(".ttml") -> "TTML"
            else -> null
        }
    }

    private fun isBinaryExecutable(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        // ELF header
        if (bytes[0] == 0x7F.toByte() && bytes[1] == 'E'.code.toByte() && bytes[2] == 'L'.code.toByte() && bytes[3] == 'F'.code.toByte()) return true
        // DOS/Windows MZ header
        if (bytes[0] == 'M'.code.toByte() && bytes[1] == 'Z'.code.toByte()) return true
        // DEX header
        if (bytes[0] == 'd'.code.toByte() && bytes[1] == 'e'.code.toByte() && bytes[2] == 'x'.code.toByte() && bytes[3] == '\n'.code.toByte()) return true
        // ZIP/APK/JAR header
        if (bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()) return true
        return false
    }
}
