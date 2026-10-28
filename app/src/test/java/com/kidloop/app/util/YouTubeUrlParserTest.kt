package com.kidloop.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeUrlParserTest {

    @Test
    fun `parse standard youtube watch url returns video`() {
        val result = YouTubeUrlParser.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertNotNull(result)
        assertTrue(result is ParsedYouTubeItem.Video)
        assertEquals("dQw4w9WgXcQ", result?.id)
    }

    @Test
    fun `parse short youtu_be url returns video`() {
        val result = YouTubeUrlParser.parse("https://youtu.be/dQw4w9WgXcQ")
        assertNotNull(result)
        assertTrue(result is ParsedYouTubeItem.Video)
        assertEquals("dQw4w9WgXcQ", result?.id)
    }

    @Test
    fun `parse mobile youtube url returns video`() {
        val result = YouTubeUrlParser.parse("https://m.youtube.com/watch?v=dQw4w9WgXcQ")
        assertNotNull(result)
        assertTrue(result is ParsedYouTubeItem.Video)
        assertEquals("dQw4w9WgXcQ", result?.id)
    }

    @Test
    fun `parse shorts url returns video`() {
        val result = YouTubeUrlParser.parse("https://www.youtube.com/shorts/dQw4w9WgXcQ")
        assertNotNull(result)
        assertTrue(result is ParsedYouTubeItem.Video)
        assertEquals("dQw4w9WgXcQ", result?.id)
    }

    @Test
    fun `parse embed url returns video`() {
        val result = YouTubeUrlParser.parse("https://www.youtube.com/embed/dQw4w9WgXcQ")
        assertNotNull(result)
        assertTrue(result is ParsedYouTubeItem.Video)
        assertEquals("dQw4w9WgXcQ", result?.id)
    }

    @Test
    fun `parse playlist url returns playlist`() {
        val result = YouTubeUrlParser.parse("https://www.youtube.com/playlist?list=PL1234567890abcdef")
        assertNotNull(result)
        assertTrue(result is ParsedYouTubeItem.Playlist)
        assertEquals("PL1234567890abcdef", result?.id)
    }

    @Test
    fun `parse url with extra query parameters extracts correct video id`() {
        val result = YouTubeUrlParser.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s&feature=share")
        assertNotNull(result)
        assertTrue(result is ParsedYouTubeItem.Video)
        assertEquals("dQw4w9WgXcQ", result?.id)
    }

    @Test
    fun `parse playlist with extra parameters extracts correct playlist id`() {
        val result = YouTubeUrlParser.parse("https://www.youtube.com/playlist?list=PL1234567890abcdef&feature=share&si=test")
        assertNotNull(result)
        assertTrue(result is ParsedYouTubeItem.Playlist)
        assertEquals("PL1234567890abcdef", result?.id)
    }

    @Test
    fun `parse raw 11-char video id succeeds`() {
        val result = YouTubeUrlParser.parse("dQw4w9WgXcQ")
        assertNotNull(result)
        assertTrue(result is ParsedYouTubeItem.Video)
        assertEquals("dQw4w9WgXcQ", result?.id)
    }

    @Test
    fun `parse invalid input returns null`() {
        assertNull(YouTubeUrlParser.parse(null))
        assertNull(YouTubeUrlParser.parse(""))
        assertNull(YouTubeUrlParser.parse("   "))
        assertNull(YouTubeUrlParser.parse("https://google.com"))
        assertNull(YouTubeUrlParser.parse("https://youtube.com/watch?v=short"))
        assertNull(YouTubeUrlParser.parse("not_a_valid_id"))
    }
}
