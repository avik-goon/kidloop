package com.kidloop.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kidloop.app.R
import com.kidloop.app.data.model.PlaylistItem
import com.kidloop.app.lock.PinLockoutManager
import com.kidloop.app.player.PlaybackStatus
import com.kidloop.app.player.PlayerError
import com.kidloop.app.player.YouTubePlayerView
import com.kidloop.app.timer.TimerState
import com.kidloop.app.ui.components.HiddenUnlockCorner
import com.kidloop.app.ui.components.PinDialog
import com.kidloop.app.ui.components.TouchLockOverlay
import com.kidloop.app.ui.theme.BreakBackground
import com.kidloop.app.ui.theme.BreakSubtext
import com.kidloop.app.ui.theme.BreakText

const val TIME_UP_SCREEN_TEST_TAG = "TimeUpScreen"
const val ALL_FAILED_SCREEN_TEST_TAG = "AllFailedScreen"
const val OFFLINE_SCREEN_TEST_TAG = "OfflineScreen"

@Composable
fun KidPlayerScreen(
    currentItem: PlaylistItem?,
    isPlaying: Boolean,
    isOffline: Boolean,
    offlineRetrySeconds: Int,
    isAllVideosFailed: Boolean,
    timerState: TimerState,
    showPinDialog: Boolean,
    lockoutManager: PinLockoutManager,
    onPlayerReady: () -> Unit,
    onPlayerStateChange: (PlaybackStatus) -> Unit,
    onPlayerError: (PlayerError) -> Unit,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    onUnlockCornerTriggered: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onPinUnlockSuccess: () -> Unit,
    onDismissPinDialog: () -> Unit,
    onRetryOnline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. YouTube WebView Player
        if (!timerState.isAnyLimitReached && !isAllVideosFailed && !isOffline) {
            YouTubePlayerView(
                currentItem = currentItem,
                isPlaying = isPlaying,
                onReady = onPlayerReady,
                onStateChange = onPlayerStateChange,
                onError = onPlayerError,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Touch Lock Overlay (intercepts all touches, detects swipe up/down & hidden corner)
            TouchLockOverlay(
                onSwipeNext = onSwipeNext,
                onSwipePrevious = onSwipePrevious,
                onUnlockTriggered = onUnlockCornerTriggered
            )
        }

        // 3. Calm Break Screen (Session or Daily Time's Up)
        if (timerState.isAnyLimitReached) {
            TimeUpScreen(
                onUnlockCornerTriggered = onUnlockCornerTriggered
            )
        }

        // 4. Calm "Ask a Parent" Screen (when all videos fail consecutively)
        if (isAllVideosFailed && !timerState.isAnyLimitReached) {
            AllVideosFailedScreen(
                onUnlockCornerTriggered = onUnlockCornerTriggered
            )
        }

        // 5. Offline Screen (when device loses network connectivity)
        if (isOffline && !timerState.isAnyLimitReached && !isAllVideosFailed) {
            OfflineScreen(
                retrySeconds = offlineRetrySeconds,
                onRetry = onRetryOnline,
                onUnlockCornerTriggered = onUnlockCornerTriggered
            )
        }

        // 6. Modal PIN Dialog (triggered by 3-second long press on hidden top-right corner)
        if (showPinDialog) {
            PinDialog(
                lockoutManager = lockoutManager,
                onVerifyPin = onVerifyPin,
                onSuccess = onPinUnlockSuccess,
                onDismiss = onDismissPinDialog
            )
        }
    }
}

/**
 * Calm, soothing screen shown when session or daily time limit expires.
 * Includes the hidden unlock corner at TopEnd so the parent can exit.
 */
@Composable
private fun TimeUpScreen(
    onUnlockCornerTriggered: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BreakBackground)
            .testTag(TIME_UP_SCREEN_TEST_TAG)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_time_up),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.time_up_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = BreakText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.time_up_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = BreakSubtext,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.time_up_parent_hint),
                style = MaterialTheme.typography.bodySmall,
                color = BreakSubtext.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }

        HiddenUnlockCorner(
            onUnlockTriggered = onUnlockCornerTriggered,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

/**
 * Calming screen shown if all videos in the playlist fail consecutively.
 */
@Composable
private fun AllVideosFailedScreen(
    onUnlockCornerTriggered: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag(ALL_FAILED_SCREEN_TEST_TAG)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_error_state),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.error_all_failed_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.error_all_failed_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.error_parent_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }

        HiddenUnlockCorner(
            onUnlockTriggered = onUnlockCornerTriggered,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

/**
 * Calm offline screen with automatic 10-second retry countdown.
 */
@Composable
private fun OfflineScreen(
    retrySeconds: Int,
    onRetry: () -> Unit,
    onUnlockCornerTriggered: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag(OFFLINE_SCREEN_TEST_TAG)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_no_internet),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.offline_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.offline_subtitle, retrySeconds),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.action_retry))
            }
        }

        HiddenUnlockCorner(
            onUnlockTriggered = onUnlockCornerTriggered,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}
