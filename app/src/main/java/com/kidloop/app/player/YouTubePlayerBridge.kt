package com.kidloop.app.player

import android.util.Log
import android.webkit.JavascriptInterface

class YouTubePlayerBridge(
    private val onReady: () -> Unit,
    private val onStateChange: (PlaybackStatus) -> Unit,
    private val onError: (PlayerError) -> Unit
) {
    @JavascriptInterface
    fun onPlayerReady() {
        Log.d("KidLoopPlayer", "Bridge: onPlayerReady")
        onReady()
    }

    @JavascriptInterface
    fun onPlayerStateChange(stateCode: Int) {
        val status = PlaybackStatus.fromCode(stateCode)
        Log.d("KidLoopPlayer", "Bridge: onPlayerStateChange -> $status (code=$stateCode)")
        onStateChange(status)
    }

    @JavascriptInterface
    fun onPlayerError(errorCode: Int) {
        val error = PlayerError.fromCode(errorCode)
        Log.e("KidLoopPlayer", "Bridge: onPlayerError -> $error (code=$errorCode)")
        onError(error)
    }
}
