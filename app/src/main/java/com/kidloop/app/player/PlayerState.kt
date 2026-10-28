package com.kidloop.app.player

/**
 * YouTube IFrame API playback states.
 * -1: UNSTARTED, 0: ENDED, 1: PLAYING, 2: PAUSED, 3: BUFFERING, 5: CUED
 */
enum class PlaybackStatus(val code: Int) {
    UNSTARTED(-1),
    ENDED(0),
    PLAYING(1),
    PAUSED(2),
    BUFFERING(3),
    CUED(5),
    UNKNOWN(-999);

    companion object {
        fun fromCode(code: Int): PlaybackStatus =
            entries.find { it.code == code } ?: UNKNOWN
    }
}

/**
 * YouTube IFrame API player error codes.
 * 2: Invalid parameter
 * 5: HTML5 player error
 * 100: Video not found or removed
 * 101/150: Video owner does not allow embedded playback
 */
enum class PlayerError(val code: Int) {
    INVALID_PARAM(2),
    HTML5_ERROR(5),
    VIDEO_NOT_FOUND(100),
    EMBED_NOT_ALLOWED(101),
    EMBED_NOT_ALLOWED_DISALLOWED(150),
    UNKNOWN(-1);

    companion object {
        fun fromCode(code: Int): PlayerError =
            entries.find { it.code == code } ?: UNKNOWN
    }
}
