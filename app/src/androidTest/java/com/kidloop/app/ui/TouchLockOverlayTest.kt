package com.kidloop.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import com.kidloop.app.ui.components.TOUCH_LOCK_OVERLAY_TEST_TAG
import com.kidloop.app.ui.components.TouchLockOverlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class TouchLockOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tapOnOverlayDoesNotTriggerSwipes() {
        var swipeNextCount = 0
        var swipePrevCount = 0
        var unlockTriggered = false

        composeTestRule.setContent {
            TouchLockOverlay(
                onSwipeNext = { swipeNextCount++ },
                onSwipePrevious = { swipePrevCount++ },
                onUnlockTriggered = { unlockTriggered = true }
            )
        }

        // Tap in the center of the overlay
        composeTestRule.onNodeWithTag(TOUCH_LOCK_OVERLAY_TEST_TAG)
            .performClick()

        assertEquals(0, swipeNextCount)
        assertEquals(0, swipePrevCount)
        assertFalse(unlockTriggered)
    }

    @Test
    fun swipeUpTriggersNextVideo() {
        var swipeNextCount = 0
        var swipePrevCount = 0

        composeTestRule.setContent {
            TouchLockOverlay(
                onSwipeNext = { swipeNextCount++ },
                onSwipePrevious = { swipePrevCount++ },
                onUnlockTriggered = {}
            )
        }

        composeTestRule.onNodeWithTag(TOUCH_LOCK_OVERLAY_TEST_TAG)
            .performTouchInput {
                swipeUp()
            }

        assertEquals(1, swipeNextCount)
        assertEquals(0, swipePrevCount)
    }

    @Test
    fun swipeDownTriggersPreviousVideo() {
        var swipeNextCount = 0
        var swipePrevCount = 0

        composeTestRule.setContent {
            TouchLockOverlay(
                onSwipeNext = { swipeNextCount++ },
                onSwipePrevious = { swipePrevCount++ },
                onUnlockTriggered = {}
            )
        }

        composeTestRule.onNodeWithTag(TOUCH_LOCK_OVERLAY_TEST_TAG)
            .performTouchInput {
                swipeDown()
            }

        assertEquals(0, swipeNextCount)
        assertEquals(1, swipePrevCount)
    }
}
