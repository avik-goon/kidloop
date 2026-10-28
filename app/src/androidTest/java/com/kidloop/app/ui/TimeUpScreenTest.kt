package com.kidloop.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.kidloop.app.data.model.ItemType
import com.kidloop.app.data.model.PlaylistItem
import com.kidloop.app.lock.PinLockoutManager
import com.kidloop.app.timer.TimerState
import com.kidloop.app.ui.components.HIDDEN_UNLOCK_TEST_TAG
import com.kidloop.app.ui.screens.KidPlayerScreen
import com.kidloop.app.ui.screens.TIME_UP_SCREEN_TEST_TAG
import org.junit.Rule
import org.junit.Test

class TimeUpScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun timeUpScreenAppearsWhenSessionExpired() {
        val dummyItem = PlaylistItem(
            id = "dQw4w9WgXcQ",
            type = ItemType.VIDEO,
            title = "Test Video",
            originalUrl = "https://youtu.be/dQw4w9WgXcQ"
        )
        val expiredTimerState = TimerState(
            sessionRemainingSeconds = 0L,
            sessionLimitSeconds = 1200L,
            dailyRemainingSeconds = 1000L,
            dailyLimitSeconds = 3600L,
            isSessionExpired = true,
            isDailyExpired = false
        )

        composeTestRule.setContent {
            KidPlayerScreen(
                currentItem = dummyItem,
                isPlaying = false,
                isOffline = false,
                offlineRetrySeconds = 10,
                isAllVideosFailed = false,
                timerState = expiredTimerState,
                showPinDialog = false,
                lockoutManager = PinLockoutManager(),
                onPlayerReady = {},
                onPlayerStateChange = {},
                onPlayerError = {},
                onSwipeNext = {},
                onSwipePrevious = {},
                onUnlockCornerTriggered = {},
                onVerifyPin = { false },
                onPinUnlockSuccess = {},
                onDismissPinDialog = {},
                onRetryOnline = {}
            )
        }

        // Verify Time Up screen is displayed
        composeTestRule.onNodeWithTag(TIME_UP_SCREEN_TEST_TAG).assertIsDisplayed()

        // Verify hidden unlock corner is still available for parent
        composeTestRule.onNodeWithTag(HIDDEN_UNLOCK_TEST_TAG).assertIsDisplayed()
    }
}
