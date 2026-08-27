package com.example.resolver

import com.example.domain.model.StreamDiagnostics
import com.example.domain.model.StreamType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class UrlAnalyzer(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    data class AnalysisResult(
        val originalUrl: String,
        val effectiveUrl: String,
        val streamType: StreamType,
        val diagnostics: StreamDiagnostics,
        val isPlayable: Boolean,
        val userErrorMessage: String? = null
    )

    suspend fun analyze(url: String, headers: Map<String, String> = emptyMap()): AnalysisResult = withContext(Dispatchers.IO) {
        val trimmedUrl = url.trim()

        if (trimmedUrl.isBlank()) {
            return@withContext AnalysisResult(
                originalUrl = url,
                effectiveUrl = url,
                streamType = StreamType.UNKNOWN,
                diagnostics = StreamDiagnostics(url = url, errorMessage = "URL is empty"),
                isPlayable = false,
                userErrorMessage = "Please enter a valid video stream URL."
            )
        }

        // Local content / file URIs
        if (trimmedUrl.startsWith("content://") || trimmedUrl.startsWith("file://")) {
            val extensionType = MediaTypeDetector.detectFromExtension(trimmedUrl)
            val detected = if (extensionType != StreamType.UNKNOWN) extensionType else StreamType.PROGRESSIVE
            return@withContext AnalysisResult(
                originalUrl = trimmedUrl,
                effectiveUrl = trimmedUrl,
                streamType = detected,
                diagnostics = StreamDiagnostics(
                    url = trimmedUrl,
                    reachable = true,
                    detectedStreamType = detected
                ),
                isPlayable = true
            )
        }

        // Validate scheme
        if (!trimmedUrl.startsWith("http://", ignoreCase = true) &&
            !trimmedUrl.startsWith("https://", ignoreCase = true)
        ) {
            return@withContext AnalysisResult(
                originalUrl = trimmedUrl,
                effectiveUrl = trimmedUrl,
                streamType = StreamType.UNKNOWN,
                diagnostics = StreamDiagnostics(url = trimmedUrl, errorMessage = "Unsupported URL scheme"),
                isPlayable = false,
                userErrorMessage = "Invalid URL protocol. Stream URLs must start with http:// or https://"
            )
        }

        // Step 1: Detect from extension first
        val fastType = MediaTypeDetector.detectFromExtension(trimmedUrl)

        // Step 2: Probe server for diagnostics & unknown format resolution
        try {
            val requestBuilder = Request.Builder()
                .url(trimmedUrl)
                .head()

            // Apply custom user headers
            headers.forEach { (key, value) ->
                if (key.isNotBlank() && value.isNotBlank()) {
                    requestBuilder.header(key, value)
                }
            }
            if (!headers.containsKey("User-Agent")) {
                requestBuilder.header("User-Agent", "TVfyyPlayer/1.0 (Android; Native Media3)")
            }

            var response = try {
                client.newCall(requestBuilder.build()).execute()
            } catch (e: Exception) {
                // If HEAD fails (e.g. 405 Method Not Allowed), try GET with Range header for 0-1024 bytes
                val getRequest = requestBuilder
                    .get()
                    .header("Range", "bytes=0-1024")
                    .build()
                client.newCall(getRequest).execute()
            }

            val statusCode = response.code
            val contentType = response.header("Content-Type")
            val finalUrl = response.request.url.toString()
            val redirects = if (finalUrl != trimmedUrl) 1 else 0

            response.close()

            val typeFromContentType = MediaTypeDetector.detectFromContentType(contentType)
            val finalType = when {
                fastType != StreamType.UNKNOWN -> fastType
                typeFromContentType != StreamType.UNKNOWN -> typeFromContentType
                finalUrl != trimmedUrl -> MediaTypeDetector.detectFromExtension(finalUrl)
                else -> StreamType.PROGRESSIVE // Fallback to progressive media for Media3
            }

            if (statusCode in 200..299 || statusCode == 206) {
                val diagnostics = StreamDiagnostics(
                    url = trimmedUrl,
                    reachable = true,
                    httpStatusCode = statusCode,
                    httpStatusMessage = response.message,
                    contentType = contentType,
                    redirectsCount = redirects,
                    finalUrl = finalUrl,
                    detectedStreamType = finalType,
                    headersSent = headers
                )
                return@withContext AnalysisResult(
                    originalUrl = trimmedUrl,
                    effectiveUrl = finalUrl,
                    streamType = finalType,
                    diagnostics = diagnostics,
                    isPlayable = true
                )
            } else {
                val errorReason = when (statusCode) {
                    401, 403 -> "Access denied (HTTP $statusCode). The server requires authorization headers or the token has expired."
                    404 -> "Media not found on server (HTTP 404). Please verify the stream URL."
                    429 -> "Too many requests (HTTP 429). The server is rate-limiting connections."
                    503 -> "Service unavailable (HTTP 503). The video server may be overloaded or requires verification."
                    else -> "Server returned HTTP $statusCode: ${response.message}"
                }

                val diagnostics = StreamDiagnostics(
                    url = trimmedUrl,
                    reachable = true,
                    httpStatusCode = statusCode,
                    httpStatusMessage = response.message,
                    contentType = contentType,
                    redirectsCount = redirects,
                    finalUrl = finalUrl,
                    detectedStreamType = finalType,
                    headersSent = headers,
                    errorMessage = errorReason
                )

                return@withContext AnalysisResult(
                    originalUrl = trimmedUrl,
                    effectiveUrl = finalUrl,
                    streamType = finalType,
                    diagnostics = diagnostics,
                    isPlayable = false,
                    userErrorMessage = errorReason
                )
            }

        } catch (e: UnknownHostException) {
            // DNS resolution failure
            val diagnostics = StreamDiagnostics(
                url = trimmedUrl,
                reachable = false,
                detectedStreamType = fastType,
                headersSent = headers,
                errorMessage = "DNS resolution failed. Unable to resolve server address."
            )
            return@withContext AnalysisResult(
                originalUrl = trimmedUrl,
                effectiveUrl = trimmedUrl,
                streamType = if (fastType != StreamType.UNKNOWN) fastType else StreamType.PROGRESSIVE,
                diagnostics = diagnostics,
                isPlayable = false,
                userErrorMessage = "Unable to connect to the video server. Please check the URL and your internet connection."
            )
        } catch (e: SocketTimeoutException) {
            val diagnostics = StreamDiagnostics(
                url = trimmedUrl,
                reachable = false,
                detectedStreamType = fastType,
                headersSent = headers,
                errorMessage = "Connection timed out after 8 seconds."
            )
            return@withContext AnalysisResult(
                originalUrl = trimmedUrl,
                effectiveUrl = trimmedUrl,
                streamType = if (fastType != StreamType.UNKNOWN) fastType else StreamType.PROGRESSIVE,
                diagnostics = diagnostics,
                isPlayable = false,
                userErrorMessage = "Connection timed out. The video server took too long to respond."
            )
        } catch (e: Exception) {
            // Network probe failed, but if fastType is known (e.g. valid HLS/DASH/MP4 url), we can still attempt Media3 playback
            val fallbackPlayable = fastType != StreamType.UNKNOWN
            val diagnostics = StreamDiagnostics(
                url = trimmedUrl,
                reachable = false,
                detectedStreamType = if (fastType != StreamType.UNKNOWN) fastType else StreamType.PROGRESSIVE,
                headersSent = headers,
                errorMessage = e.localizedMessage ?: "Unknown network error"
            )
            return@withContext AnalysisResult(
                originalUrl = trimmedUrl,
                effectiveUrl = trimmedUrl,
                streamType = if (fastType != StreamType.UNKNOWN) fastType else StreamType.PROGRESSIVE,
                diagnostics = diagnostics,
                isPlayable = fallbackPlayable,
                userErrorMessage = if (!fallbackPlayable) "Failed to reach video stream: ${e.localizedMessage}" else null
            )
        }
    }
}
