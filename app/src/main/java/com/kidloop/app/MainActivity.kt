package com.kidloop.app

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.kidloop.app.lock.LockManager
import com.kidloop.app.ui.screens.KidPlayerScreen
import com.kidloop.app.ui.screens.ParentExplanationScreen
import com.kidloop.app.ui.viewmodel.ScreenState
import com.kidloop.app.ui.screens.SettingsScreen
import com.kidloop.app.ui.theme.KidLoopTheme
import com.kidloop.app.ui.viewmodel.KidLoopViewModel
import com.kidloop.app.ui.viewmodel.KidLoopViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: KidLoopViewModel by viewModels {
        KidLoopViewModelFactory(applicationContext)
    }

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Intentionally consumed and blocked while Kid Mode is active
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Block hardware/gesture back press when locked
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        // Enable FLAG_SECURE in release builds to protect privacy
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!isDebuggable) {
            LockManager.setSecureFlag(window, true)
        }

        setContent {
            KidLoopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()

                    // Enable/disable back press blocking based on Kid Mode state
                    val isKidModeActive = uiState.screen == ScreenState.KID_MODE
                    LaunchedEffect(isKidModeActive) {
                        backPressedCallback.isEnabled = isKidModeActive
                        if (isKidModeActive) {
                            LockManager.applyImmersiveMode(window)
                            LockManager.setKeepScreenOn(window, true)
                        } else {
                            LockManager.restoreSystemBars(window)
                            LockManager.setKeepScreenOn(window, false)
                        }
                    }

                    // Keep screen on only while playing
                    LaunchedEffect(uiState.isPlaying, uiState.timerState.isAnyLimitReached) {
                        val shouldKeepOn = isKidModeActive && uiState.isPlaying && !uiState.timerState.isAnyLimitReached
                        LockManager.setKeepScreenOn(window, shouldKeepOn)
                    }

                    when (uiState.screen) {
                        ScreenState.SETTINGS -> {
                            SettingsScreen(
                                settings = uiState.settings,
                                dailyUsage = uiState.dailyUsage,
                                onAddLink = { viewModel.addLink(it) },
                                onRemoveLink = { viewModel.removeLink(it) },
                                onReorderLink = { from, to -> viewModel.reorderLink(from, to) },
                                onSavePin = { viewModel.savePin(it) },
                                onUpdateSessionLimit = { viewModel.updateSessionLimit(it) },
                                onUpdateDailyLimit = { viewModel.updateDailyLimit(it) },
                                onResetTodayUsage = { viewModel.resetTodayUsage() },
                                onToggleAllowVolume = { viewModel.toggleAllowVolumeKeys(it) },
                                onStartKidMode = { viewModel.onStartKidModeClicked() }
                            )
                        }

                        ScreenState.EXPLANATION -> {
                            ParentExplanationScreen(
                                isPinningDeclined = uiState.isPinningDeclined,
                                onConfirmLaunch = {
                                    LockManager.startLockTask(this@MainActivity)
                                    val isPinned = LockManager.isScreenPinned(this@MainActivity)
                                    viewModel.setPinningDeclined(!isPinned)
                                    viewModel.onConfirmLaunchKidMode()
                                },
                                onCancel = { viewModel.onCancelExplanation() }
                            )
                        }

                        ScreenState.KID_MODE -> {
                            KidPlayerScreen(
                                currentItem = uiState.currentItem,
                                isPlaying = uiState.isPlaying,
                                isOffline = uiState.isOffline,
                                offlineRetrySeconds = uiState.offlineRetrySeconds,
                                isAllVideosFailed = uiState.isAllVideosFailed,
                                timerState = uiState.timerState,
                                showPinDialog = uiState.showPinDialog,
                                lockoutManager = viewModel.lockoutManager,
                                onPlayerReady = { viewModel.onPlayerReady() },
                                onPlayerStateChange = { viewModel.onPlayerStateChange(it) },
                                onPlayerError = { viewModel.onPlayerError(it) },
                                onSwipeNext = { viewModel.nextVideo() },
                                onSwipePrevious = { viewModel.previousVideo() },
                                onUnlockCornerTriggered = { viewModel.onUnlockCornerTriggered() },
                                onVerifyPin = { viewModel.verifyPin(it) },
                                onPinUnlockSuccess = {
                                    LockManager.stopLockTask(this@MainActivity)
                                    viewModel.onPinUnlockSuccess()
                                },
                                onDismissPinDialog = { viewModel.onDismissPinDialog() },
                                onRetryOnline = { viewModel.retryOnlineNow() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && viewModel.uiState.value.screen == ScreenState.KID_MODE) {
            LockManager.applyImmersiveMode(window)

            // Re-check screen pinning if unexpectedly unpinned during active Kid Mode
            if (!LockManager.isScreenPinned(this)) {
                viewModel.setPinningDeclined(true)
            }
        }
    }

    /**
     * Blocks physical volume keys if parent disabled "Allow Volume Keys" in settings.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isKidModeActive = viewModel.uiState.value.screen == ScreenState.KID_MODE
        val allowVolume = viewModel.uiState.value.settings.allowVolumeKeys

        if (isKidModeActive && !allowVolume) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE -> {
                    // Consume key event without altering system volume
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        LockManager.stopLockTask(this)
    }
}
