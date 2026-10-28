package com.kidloop.app.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.kidloop.app.data.model.ItemType
import com.kidloop.app.data.model.PlaylistItem

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun YouTubePlayerView(
    currentItem: PlaylistItem?,
    isPlaying: Boolean,
    onReady: () -> Unit,
    onStateChange: (PlaybackStatus) -> Unit,
    onError: (PlayerError) -> Unit,
    modifier: Modifier = Modifier
) {
    val bridge = remember {
        YouTubePlayerBridge(
            onReady = onReady,
            onStateChange = onStateChange,
            onError = onError
        )
    }

    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            createLockedWebView(context, bridge).also { wv ->
                webViewRef.value = wv
                if (currentItem != null) {
                    val html = YouTubeHtmlBuilder.build(
                        initialItemId = currentItem.id,
                        isPlaylist = currentItem.type == ItemType.PLAYLIST
                    )
                    Log.d("KidLoopPlayer", "Loading HTML for item: ${currentItem.id}, type: ${currentItem.type}")
                    wv.loadDataWithBaseURL(
                        "https://com.kidloop.app",
                        html,
                        "text/html",
                        "utf-8",
                        null
                    )
                }
            }
        },
        update = { wv ->
            webViewRef.value = wv
        },
        onRelease = { wv ->
            cleanupWebView(wv)
        }
    )

    // Handle playlist/video item changes
    LaunchedEffect(currentItem?.id) {
        val item = currentItem ?: return@LaunchedEffect
        val wv = webViewRef.value ?: return@LaunchedEffect
        Log.d("KidLoopPlayer", "LaunchedEffect currentItem changed: ${item.id}")
        if (item.type == ItemType.PLAYLIST) {
            wv.evaluateJavascript("if (typeof loadPlaylist === 'function') { loadPlaylist('${item.id}'); }", null)
        } else {
            wv.evaluateJavascript("if (typeof loadVideo === 'function') { loadVideo('${item.id}'); }", null)
        }
    }

    // Handle play / pause triggers
    LaunchedEffect(isPlaying) {
        val wv = webViewRef.value ?: return@LaunchedEffect
        Log.d("KidLoopPlayer", "LaunchedEffect isPlaying changed: $isPlaying")
        if (isPlaying) {
            wv.evaluateJavascript("if (typeof play === 'function') { play(); }", null)
        } else {
            wv.evaluateJavascript("if (typeof pause === 'function') { pause(); }", null)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
private fun createLockedWebView(context: Context, bridge: YouTubePlayerBridge): WebView {
    WebView.setWebContentsDebuggingEnabled(true)
    return WebView(context).apply {
        setBackgroundColor(Color.BLACK)

        // Block touches at WebView level (Compose overlay handles allowed gestures)
        isClickable = false
        isFocusable = false
        isFocusableInTouchMode = false
        isLongClickable = false
        setOnTouchListener { _, _ -> true }

        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER

        settings.apply {
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false

            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false

            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        addJavascriptInterface(bridge, "KidLoopBridge")

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val isMainFrame = request?.isForMainFrame ?: false
                if (isMainFrame) {
                    val url = request?.url?.toString().orEmpty()
                    if (!url.startsWith("https://com.kidloop.app")) {
                        Log.d("KidLoopPlayer", "Blocked main-frame navigation to: $url")
                        return true
                    }
                }
                return false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    Log.w("KidLoopPlayer", "Main frame error: ${error?.description} on ${request.url}")
                }
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                if (consoleMessage != null) {
                    Log.d("KidLoopPlayer", "JS Console: [${consoleMessage.messageLevel()}] ${consoleMessage.message()} (line ${consoleMessage.lineNumber()})")
                }
                return true
            }
        }
    }
}

fun cleanupWebView(webView: WebView) {
    try {
        webView.removeJavascriptInterface("KidLoopBridge")
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.clearHistory()
        webView.removeAllViews()
        webView.destroy()
    } catch (e: Exception) {
        Log.w("KidLoopPlayer", "cleanupWebView exception: ${e.message}")
    }
}
