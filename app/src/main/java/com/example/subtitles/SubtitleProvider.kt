package com.example.subtitles

import com.example.domain.model.OnlineSubtitleItem
import com.example.domain.model.SubtitleSearchQuery
import java.io.File

interface SubtitleProvider {
    val providerName: String

    suspend fun searchSubtitles(query: SubtitleSearchQuery): Result<List<OnlineSubtitleItem>>

    suspend fun downloadSubtitle(
        item: OnlineSubtitleItem,
        destinationFile: File,
        onProgress: (Float) -> Unit
    ): Result<File>
}
