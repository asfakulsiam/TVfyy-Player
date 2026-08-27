package com.example.player

import androidx.media3.common.Player
import com.example.domain.model.MediaItemData
import com.example.domain.model.PlaybackState
import com.example.domain.model.StreamDiagnostics
import com.example.domain.model.SubtitleStyleConfig
import com.example.domain.model.TrackInfo
import kotlinx.coroutines.flow.StateFlow

interface PlayerEngine {
    val playbackState: StateFlow<PlaybackState>
    val videoTracks: StateFlow<List<TrackInfo>>
    val audioTracks: StateFlow<List<TrackInfo>>
    val subtitleTracks: StateFlow<List<TrackInfo>>
    val streamDiagnostics: StateFlow<StreamDiagnostics>
    val activeCuesText: StateFlow<String?>

    fun prepare(mediaItemData: MediaItemData)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun seekBy(deltaMs: Long)
    fun jumpToLiveEdge()
    fun setPlaybackSpeed(speed: Float)
    fun selectTrack(trackInfo: TrackInfo?)
    fun selectAutoQuality()
    fun selectSubtitleTrack(trackInfo: TrackInfo?)
    fun addExternalSubtitleTrack(trackInfo: TrackInfo)
    fun disableSubtitles()
    fun setVolume(volume: Float)
    fun setVolumeBoost(percent: Int) // 100, 125, 150
    fun setAudioDelay(delayMs: Long)
    fun setSubtitleDelay(delayMs: Long)
    fun setPreferredLanguages(audioLang: String?, subtitleLang: String?)
    fun retry()
    fun release()
    fun getPlayer(): Player?
}
