package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.PlaybackState
import com.example.domain.model.StreamDiagnostics
import com.example.domain.model.StreamType
import com.example.domain.model.SubtitleFontSize
import com.example.domain.model.SubtitlePosition
import com.example.domain.model.SubtitleSearchQuery
import com.example.domain.model.SubtitleSource
import com.example.domain.model.SubtitleStyleConfig
import com.example.domain.model.TrackInfo
import com.example.domain.model.TrackType
import com.example.resolver.MediaTypeDetector
import com.example.subtitles.CommunitySubtitleProvider
import com.example.subtitles.MediaTitleParser
import com.example.subtitles.SubtitleFileValidator
import com.example.ui.player.components.TimeFormatter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TVfyy Player", appName)
    }

    @Test
    fun `detect stream types correctly`() {
        assertEquals(StreamType.HLS, MediaTypeDetector.detectFromExtension("https://example.com/live.m3u8"))
        assertEquals(StreamType.DASH, MediaTypeDetector.detectFromExtension("https://example.com/stream.mpd"))
        assertEquals(StreamType.PROGRESSIVE, MediaTypeDetector.detectFromExtension("https://example.com/video.mp4"))
        assertEquals(StreamType.PROGRESSIVE, MediaTypeDetector.detectFromExtension("https://example.com/movie.mkv"))
        assertEquals(StreamType.PROGRESSIVE, MediaTypeDetector.detectFromExtension("https://example.com/clip.webm"))
    }

    @Test
    fun `time formatting works correctly`() {
        assertEquals("00:00", TimeFormatter.formatMs(0))
        assertEquals("00:15", TimeFormatter.formatMs(15000))
        assertEquals("01:15", TimeFormatter.formatMs(75000))
        assertEquals("01:01:05", TimeFormatter.formatMs(3665000))
    }

    @Test
    fun `media title parser cleans release tags and extracts metadata`() {
        val parsed1 = MediaTitleParser.parse("Game.of.Thrones.S01E05.1080p.BluRay.x264-TVfyy.mkv")
        assertEquals("Game Of Thrones", parsed1.cleanTitle)
        assertEquals(1, parsed1.season)
        assertEquals(5, parsed1.episode)

        val parsed2 = MediaTitleParser.parse("The.Matrix.1999.2160p.UHD.HDR.DDP5.1.mp4")
        assertEquals("The Matrix", parsed2.cleanTitle)
        assertEquals("1999", parsed2.year)

        val parsed3 = MediaTitleParser.parse("https://cdn.example.com/vod/Inception_2010_1080p.mp4?token=123")
        assertEquals("Inception", parsed3.cleanTitle)
        assertEquals("2010", parsed3.year)
    }

    @Test
    fun `subtitle file validator detects mime types and format names`() {
        assertEquals("text/vtt", SubtitleFileValidator.detectMimeType("test.vtt"))
        assertEquals("application/x-subrip", SubtitleFileValidator.detectMimeType("movie_english.srt"))
        assertEquals("text/x-ssa", SubtitleFileValidator.detectMimeType("anime.ass"))

        assertEquals("WebVTT", SubtitleFileValidator.detectFormatName("text/vtt"))
        assertEquals("SubRip (SRT)", SubtitleFileValidator.detectFormatName("application/x-subrip"))
        assertEquals("SubStation Alpha (ASS)", SubtitleFileValidator.detectFormatName("text/x-ssa"))
    }

    @Test
    fun `online subtitle provider searches and handles query gracefully`() = runBlocking {
        val provider = CommunitySubtitleProvider()
        val emptyQuery = SubtitleSearchQuery(title = "", year = "2010", language = "bn")
        val result = provider.searchSubtitles(emptyQuery)

        assertTrue(result.isSuccess)
        val items = result.getOrNull()
        assertNotNull(items)
        assertTrue(items!!.isEmpty())
    }

    @Test
    fun `subtitle style config calculation`() {
        val style = SubtitleStyleConfig(
            fontSize = SubtitleFontSize.LARGE,
            position = SubtitlePosition.BOTTOM,
            customVerticalOffsetPercent = -0.1f,
            customScaleFactor = 1.2f
        )
        assertEquals(28.8f, style.effectiveFontSizeSp, 0.01f)
        assertEquals(0.78f, style.effectiveVerticalPercent, 0.01f)
    }

    @Test
    fun `stream diagnostics masked url hides sensitive tokens`() {
        val diag = StreamDiagnostics(
            url = "https://stream.provider.com/live/ch1.m3u8?token=secret123456789&auth=admin"
        )
        val masked = diag.maskedUrl
        assertFalse(masked.contains("secret123456789"))
        assertTrue(masked.contains("token=********"))
        assertTrue(masked.contains("auth=********"))
    }

    @Test
    fun `track info formatting produces clear audio labels and badges`() {
        val audioTrack = TrackInfo(
            id = "audio_1",
            trackGroupIndex = 0,
            trackIndex = 0,
            type = TrackType.AUDIO,
            label = "English • Dolby Digital Plus (E-AC-3) (5.1 Surround)",
            language = "English",
            languageCode = "en",
            codecs = "Dolby Digital Plus (E-AC-3)",
            channelCount = 6,
            channelConfiguration = "5.1 Surround",
            bitrate = 384000,
            isOriginal = true,
            isSelected = true
        )
        assertEquals("English", audioTrack.language)
        assertEquals("Dolby Digital Plus (E-AC-3) • 5.1 Surround • 384 kbps", audioTrack.audioDetailsLabel)
        assertTrue(audioTrack.isOriginal)
        assertTrue(audioTrack.isSelected)

        val subTrack = TrackInfo(
            id = "sub_1",
            trackGroupIndex = 0,
            trackIndex = 0,
            type = TrackType.SUBTITLE,
            label = "Bangla (Bengali) [SDH] (WebVTT)",
            language = "Bangla (Bengali)",
            languageCode = "bn",
            formatName = "WebVTT",
            source = SubtitleSource.EMBEDDED,
            isSDH = true,
            isSelected = true
        )
        assertTrue(subTrack.isSDH)
        assertEquals("WebVTT", subTrack.formatName)
    }

    @Test
    fun `playback state default calculations`() {
        val state = PlaybackState(
            isPlaying = true,
            currentPositionMs = 30000,
            durationMs = 60000,
            bufferedPositionMs = 45000
        )
        assertEquals(0.5f, state.progress, 0.001f)
        assertEquals(0.75f, state.bufferedProgress, 0.001f)
    }

    @Test
    fun `support profile matches exact requirements`() {
        val dev = com.example.domain.model.SupportProfile.developer
        assertEquals("ASFAKUL SIAM", dev.name)
        assertEquals("BANGLADESH", dev.country)
        assertEquals("asfakulsiam", dev.instagramUsername)
        assertEquals("asfakulsiam", dev.githubUsername)
        assertEquals("https://asfakulsiam.dev.cv", dev.portfolioUrl)

        val mb = com.example.domain.model.SupportProfile.mobileBanking
        assertEquals("01734737294", mb.number)
        assertEquals("Personal", mb.accountType)

        val bank = com.example.domain.model.SupportProfile.bankTransfer
        assertEquals("DUTCH-BANGLA BANK", bank.bankName)
        assertEquals("ASFAKUL ISLAM", bank.accountName)
        assertEquals("2067348739614", bank.accountNumber)
        assertEquals("Jamalpur Branch", bank.branch)
        assertEquals("090390854", bank.routingNumber)

        val formattedBank = bank.toFormattedDetails()
        assertTrue(formattedBank.contains("DUTCH-BANGLA BANK"))
        assertTrue(formattedBank.contains("2067348739614"))
        assertTrue(formattedBank.contains("090390854"))

        val btc = com.example.domain.model.SupportProfile.cryptoWallets.first { it.symbol == "BTC" }
        assertEquals("bc1p36fvxlef8apl0c3vnu2hx286hfj57p2zqxzxxcksfgn3rq34zmasen754u", btc.address)
        assertEquals("Bitcoin", btc.network)

        val usdt = com.example.domain.model.SupportProfile.cryptoWallets.first { it.symbol == "USDT" }
        assertEquals("0x245125F8C5D3c814c4b1d1a3604160a39C21d0d2", usdt.address)
        assertEquals("BNB Chain", usdt.network)

        val allDetails = com.example.domain.model.SupportProfile.getAllPaymentDetailsText()
        assertTrue(allDetails.contains("ASFAKUL SIAM"))
        assertTrue(allDetails.contains("01734737294"))
        assertTrue(allDetails.contains("asfakulsiam0@gmail.com"))
    }

    @Test
    fun `support preferences never show again and prompt throttling`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.local.SupportPreferences(context)
        
        assertFalse(prefs.isNeverShowAgain())
        
        // Not enough continuous watch seconds
        assertFalse(prefs.canShowContextualPrompt(sessionWatchSeconds = 20))
        
        // Eligible with >= 45s
        assertTrue(prefs.canShowContextualPrompt(sessionWatchSeconds = 50))
        
        // After recording shown, cooldown blocks subsequent prompts
        prefs.recordPromptShown()
        assertFalse(prefs.canShowContextualPrompt(sessionWatchSeconds = 60))
        
        // Never show again permanently blocks
        prefs.setNeverShowAgain(true)
        assertTrue(prefs.isNeverShowAgain())
        assertFalse(prefs.canShowContextualPrompt(sessionWatchSeconds = 120))
    }
}
