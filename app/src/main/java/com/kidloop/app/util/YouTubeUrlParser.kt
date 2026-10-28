package com.kidloop.app.util

import java.net.URI

sealed interface ParsedYouTubeItem {
    val id: String
    val originalUrl: String

    data class Video(
        override val id: String,
        override val originalUrl: String
    ) : ParsedYouTubeItem

    data class Playlist(
        override val id: String,
        override val originalUrl: String
    ) : ParsedYouTubeItem
}

object YouTubeUrlParser {

    private val VIDEO_ID_REGEX = Regex("^[a-zA-Z0-9_-]{11}$")
    private val PLAYLIST_ID_REGEX = Regex("^[a-zA-Z0-9_-]{12,}$")

    /**
     * Parses and validates a YouTube video or playlist URL.
     * Supports:
     * - https://www.youtube.com/watch?v=VIDEO_ID
     * - https://youtu.be/VIDEO_ID
     * - https://m.youtube.com/watch?v=VIDEO_ID
     * - https://www.youtube.com/shorts/VIDEO_ID
     * - https://www.youtube.com/embed/VIDEO_ID
     * - https://www.youtube.com/playlist?list=PLAYLIST_ID
     * - Raw 11-char video IDs or valid playlist IDs.
     *
     * Pure Kotlin/JVM implementation using [java.net.URI] so it runs reliably
     * in both JVM unit tests and Android runtime without mocked dependencies.
     */
    fun parse(rawInput: String?): ParsedYouTubeItem? {
        if (rawInput.isNullOrBlank()) return null
        val trimmed = rawInput.trim()

        // Check if raw string is already an 11-character video ID
        if (VIDEO_ID_REGEX.matches(trimmed)) {
            return ParsedYouTubeItem.Video(id = trimmed, originalUrl = trimmed)
        }

        val uri = try {
            val formatted = if (!trimmed.startsWith("http://", ignoreCase = true) &&
                !trimmed.startsWith("https://", ignoreCase = true)
            ) {
                "https://$trimmed"
            } else {
                trimmed
            }
            URI(formatted)
        } catch (_: Exception) {
            return null
        }

        val host = uri.host?.lowercase() ?: return null
        val path = uri.path.orEmpty()
        val queryParams = parseQueryParams(uri.rawQuery)

        // Check if host belongs to YouTube domains
        val isYouTubeHost = host == "youtube.com" ||
                host.endsWith(".youtube.com") ||
                host == "youtu.be"

        if (!isYouTubeHost) return null

        // 1. Playlist URL: path contains /playlist or list parameter exists without v
        if (path.contains("/playlist") || (queryParams.containsKey("list") && !queryParams.containsKey("v"))) {
            val listParam = queryParams["list"]
            if (listParam != null && PLAYLIST_ID_REGEX.matches(listParam)) {
                return ParsedYouTubeItem.Playlist(id = listParam, originalUrl = trimmed)
            }
        }

        // 2. Short link: youtu.be/VIDEO_ID
        if (host == "youtu.be") {
            val segment = path.removePrefix("/").split("/").firstOrNull().orEmpty()
            if (VIDEO_ID_REGEX.matches(segment)) {
                return ParsedYouTubeItem.Video(id = segment, originalUrl = trimmed)
            }
        }

        // 3. Shorts URL: /shorts/VIDEO_ID
        if (path.contains("/shorts/")) {
            val segment = path.substringAfter("/shorts/").split("/").firstOrNull().orEmpty()
            if (VIDEO_ID_REGEX.matches(segment)) {
                return ParsedYouTubeItem.Video(id = segment, originalUrl = trimmed)
            }
        }

        // 4. Embed URL: /embed/VIDEO_ID
        if (path.contains("/embed/")) {
            val segment = path.substringAfter("/embed/").split("/").firstOrNull().orEmpty()
            if (VIDEO_ID_REGEX.matches(segment)) {
                return ParsedYouTubeItem.Video(id = segment, originalUrl = trimmed)
            }
        }

        // 5. Standard watch URL: /watch?v=VIDEO_ID
        val vParam = queryParams["v"]
        if (vParam != null && VIDEO_ID_REGEX.matches(vParam)) {
            return ParsedYouTubeItem.Video(id = vParam, originalUrl = trimmed)
        }

        // 6. Watch URL containing list fallback: if list parameter is present
        val listFallback = queryParams["list"]
        if (listFallback != null && PLAYLIST_ID_REGEX.matches(listFallback)) {
            return ParsedYouTubeItem.Playlist(id = listFallback, originalUrl = trimmed)
        }

        return null
    }

    private fun parseQueryParams(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        val pairs = rawQuery.split("&")
        for (pair in pairs) {
            val parts = pair.split("=", limit = 2)
            if (parts.isNotEmpty()) {
                val key = parts[0]
                val value = if (parts.size > 1) parts[1] else ""
                result[key] = value
            }
        }
        return result
    }
}
