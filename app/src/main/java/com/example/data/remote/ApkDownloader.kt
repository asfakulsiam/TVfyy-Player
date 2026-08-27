package com.example.data.remote

import android.content.Context
import com.example.domain.model.DownloadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

class ApkDownloader(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    fun downloadApk(url: String, versionTag: String): Flow<DownloadState> = flow {
        emit(
            DownloadState.Downloading(
                bytesDownloaded = 0L,
                totalBytes = 0L,
                progressPercent = 0,
                downloadedFormatted = "0.0 MB",
                totalFormatted = "Calculating...",
                speedFormatted = "Starting...",
                isIndeterminate = true
            )
        )

        val updatesDir = File(context.cacheDir, "updates").apply {
            if (!exists()) mkdirs()
        }

        val cleanTag = versionTag.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        val apkFile = File(updatesDir, "TVfyy-Player-$cleanTag.apk")
        val tempFile = File(updatesDir, "TVfyy-Player-$cleanTag.apk.tmp")

        // If exact apk was already downloaded and is valid, we can verify and return immediately
        if (apkFile.exists() && apkFile.length() > 5 * 1024 * 1024) {
            emit(
                DownloadState.Completed(
                    apkFile = apkFile,
                    versionTag = versionTag,
                    fileSizeBytes = apkFile.length()
                )
            )
            return@flow
        }

        if (tempFile.exists()) {
            tempFile.delete()
        }

        var outputStream: FileOutputStream? = null

        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "TVfyy-Player-Android-App")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(DownloadState.Error("Server returned HTTP ${response.code} when downloading update."))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(DownloadState.Error("Received empty response from server."))
                return@flow
            }

            val totalBytes = body.contentLength()
            val totalFormatted = if (totalBytes > 0) formatBytes(totalBytes) else "Unknown"

            val inputStream = body.byteStream()
            outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            var totalDownloaded = 0L
            var lastEmittedTime = System.currentTimeMillis()
            var bytesSinceLastCalc = 0L
            var speedFormatted = ""

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (!currentCoroutineContext().isActive) {
                    throw CancellationException("Download cancelled by user.")
                }

                outputStream.write(buffer, 0, bytesRead)
                totalDownloaded += bytesRead
                bytesSinceLastCalc += bytesRead

                val now = System.currentTimeMillis()
                val elapsed = now - lastEmittedTime

                if (elapsed >= 300 || (totalBytes > 0 && totalDownloaded == totalBytes)) {
                    val speedBytesPerSec = if (elapsed > 0) (bytesSinceLastCalc * 1000.0) / elapsed else 0.0
                    speedFormatted = formatSpeed(speedBytesPerSec)
                    bytesSinceLastCalc = 0L
                    lastEmittedTime = now

                    val progressPercent = if (totalBytes > 0) {
                        ((totalDownloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                    } else {
                        0
                    }

                    emit(
                        DownloadState.Downloading(
                            bytesDownloaded = totalDownloaded,
                            totalBytes = totalBytes,
                            progressPercent = progressPercent,
                            downloadedFormatted = formatBytes(totalDownloaded),
                            totalFormatted = totalFormatted,
                            speedFormatted = speedFormatted,
                            isIndeterminate = totalBytes <= 0
                        )
                    )
                }
            }

            outputStream.flush()
            outputStream.close()
            outputStream = null

            // Atomically rename temp file to final APK file
            if (apkFile.exists()) {
                apkFile.delete()
            }
            val renamed = tempFile.renameTo(apkFile)
            val finalFile = if (renamed) apkFile else tempFile

            emit(
                DownloadState.Completed(
                    apkFile = finalFile,
                    versionTag = versionTag,
                    fileSizeBytes = finalFile.length()
                )
            )
        } catch (e: CancellationException) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            emit(DownloadState.Error(e.localizedMessage ?: "Failed to download update APK."))
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0.0 MB"
            val mb = bytes / (1024.0 * 1024.0)
            return String.format(Locale.US, "%.1f MB", mb)
        }

        fun formatSpeed(bytesPerSec: Double): String {
            if (bytesPerSec <= 0) return ""
            val kbPerSec = bytesPerSec / 1024.0
            return if (kbPerSec >= 1024.0) {
                String.format(Locale.US, "%.1f MB/s", kbPerSec / 1024.0)
            } else {
                String.format(Locale.US, "%.0f KB/s", kbPerSec)
            }
        }
    }
}
