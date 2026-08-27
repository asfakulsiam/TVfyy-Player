package com.example

import com.example.data.parser.M3uExportOptions
import com.example.data.parser.M3uExporter
import com.example.data.parser.M3uParser
import com.example.domain.model.Playlist
import com.example.domain.model.PlaylistChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistParserAndDiffTest {

    @Test
    fun testParseStandardM3u() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="f27de012-0785-4152-892a-014a497e234e" tvg-name="Unite8 Sports 1" tvg-logo="https://example.com/logo.png" group-title="Sports",Unite8 Sports 1
            https://example.com/stream1.m3u8
            
            #EXTINF:-1 tvg-id="e9943a0d-a64f-4da6-9a12-023d604dc6b1" tvg-name="Unite8 Sports 2" tvg-logo="https://example.com/logo2.png" group-title="Sports",Unite8 Sports 2
            https://example.com/stream2.m3u8
        """.trimIndent()

        val parsed = M3uParser.parse(m3uContent, "Sports Playlist")

        assertEquals("Sports Playlist", parsed.defaultName)
        assertEquals(2, parsed.entries.size)
        assertEquals(1, parsed.categories.size)
        assertEquals("Sports", parsed.categories[0])

        val ch1 = parsed.entries[0]
        assertEquals("Unite8 Sports 1", ch1.name)
        assertEquals("https://example.com/stream1.m3u8", ch1.streamUrl)
        assertEquals("f27de012-0785-4152-892a-014a497e234e", ch1.tvgId)
        assertEquals("Unite8 Sports 1", ch1.tvgName)
        assertEquals("https://example.com/logo.png", ch1.tvgLogo)
        assertEquals("Sports", ch1.groupTitle)
    }

    @Test
    fun testParsePreservesUnknownAttributes() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="ch-99" custom-attr="custom_value" user-agent="CustomAgent/1.0" group-title="News",BBC News
            https://example.com/bbc.m3u8
        """.trimIndent()

        val parsed = M3uParser.parse(m3uContent)
        val entry = parsed.entries.first()

        assertEquals("BBC News", entry.name)
        assertEquals("ch-99", entry.tvgId)
        assertEquals("News", entry.groupTitle)
        assertEquals("custom_value", entry.unknownAttributes["custom-attr"])
        assertEquals("CustomAgent/1.0", entry.knownAttributes["user-agent"])

        val channel = PlaylistChannel(
            playlistId = 1L,
            name = entry.name,
            streamUrl = entry.streamUrl,
            tvgId = entry.tvgId,
            categoryName = entry.groupTitle ?: "Uncategorized",
            unknownAttributes = entry.unknownAttributes
        )

        // Test Round-Trip Export
        val exported = M3uExporter.exportToString(
            playlist = Playlist(name = "Test", channelCount = 1),
            channels = listOf(channel),
            options = M3uExportOptions(includeCustomAttributes = true)
        )

        assertTrue(exported.contains("custom-attr=\"custom_value\""))
        assertTrue(exported.contains("https://example.com/bbc.m3u8"))
    }

    @Test
    fun testParseUnicodeAndBangla() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 group-title="বাংলাদেশ",সময় টিভি (Somoy TV)
            https://example.com/somoy.m3u8
            #EXTINF:-1 group-title="বাংলা বিনোদন",চ্যানেল আই 📺
            https://example.com/channel_i.m3u8
        """.trimIndent()

        val parsed = M3uParser.parse(m3uContent)
        assertEquals(2, parsed.entries.size)
        assertEquals("সময় টিভি (Somoy TV)", parsed.entries[0].name)
        assertEquals("বাংলাদেশ", parsed.entries[0].groupTitle)
        assertEquals("চ্যানেল আই 📺", parsed.entries[1].name)
    }

    @Test
    fun testToleranceForMissingExtM3uHeader() {
        val m3uContent = """
            #EXTINF:-1 group-title="Music",MTV Hits
            https://example.com/mtv.m3u8
            https://example.com/standalone_stream.mp4
        """.trimIndent()

        val parsed = M3uParser.parse(m3uContent)
        assertEquals(2, parsed.entries.size)
        assertEquals("MTV Hits", parsed.entries[0].name)
        assertEquals("Standalone Stream", parsed.entries[1].name)
    }
}
