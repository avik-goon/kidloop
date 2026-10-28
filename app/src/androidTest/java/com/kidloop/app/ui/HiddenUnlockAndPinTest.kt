package com.kidloop.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.kidloop.app.lock.PinLockoutManager
import com.kidloop.app.ui.components.HIDDEN_UNLOCK_TEST_TAG
import com.kidloop.app.ui.components.HiddenUnlockCorner
import com.kidloop.app.ui.components.PIN_DIALOG_TEST_TAG
import com.kidloop.app.ui.components.PIN_ERROR_TEST_TAG
import com.kidloop.app.ui.components.PIN_SUBMIT_TEST_TAG
import com.kidloop.app.ui.components.PinDialog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HiddenUnlockAndPinTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun longPressingHiddenCornerFor3SecondsTriggersUnlock() {
        var unlockTriggered = false

        composeTestRule.setContent {
            HiddenUnlockCorner(
                onUnlockTriggered = { unlockTriggered = true }
            )
        }

        // Hold touch for 3100ms
        composeTestRule.onNodeWithTag(HIDDEN_UNLOCK_TEST_TAG)
            .performTouchInput {
                down(center)
                advanceEventTime(3100L)
                up()
            }

        assertTrue(unlockTriggered)
    }

    @Test
    fun shortPressOnHiddenCornerDoesNotTriggerUnlock() {
        var unlockTriggered = false

        composeTestRule.setContent {
            HiddenUnlockCorner(
                onUnlockTriggered = { unlockTriggered = true }
            )
        }

        // Tap for only 500ms
        composeTestRule.onNodeWithTag(HIDDEN_UNLOCK_TEST_TAG)
            .performTouchInput {
                down(center)
                advanceEventTime(500L)
                up()
            }

        assertFalse(unlockTriggered)
    }

    @Test
    fun wrongPinShowsErrorMessageAndDecrementsAttempts() {
        val lockoutManager = PinLockoutManager()
        var unlockSuccess = false

        composeTestRule.setContent {
            PinDialog(
                lockoutManager = lockoutManager,
                onVerifyPin = { entered -> entered == "1234" },
                onSuccess = { unlockSuccess = true },
                onDismiss = {}
            )
        }

        // Enter wrong pin "9999"
        composeTestRule.onNodeWithText("9").performClick()
        composeTestRule.onNodeWithText("9").performClick()
        composeTestRule.onNodeWithText("9").performClick()
        composeTestRule.onNodeWithText("9").performClick()

        // Submit
        composeTestRule.onNodeWithTag(PIN_SUBMIT_TEST_TAG).performClick()

        assertFalse(unlockSuccess)
        composeTestRule.onNodeWithTag(PIN_ERROR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun correctPinTriggersSuccessCallback() {
        val lockoutManager = PinLockoutManager()
        var unlockSuccess = false

        composeTestRule.setContent {
            PinDialog(
                lockoutManager = lockoutManager,
                onVerifyPin = { entered -> entered == "1234" },
                onSuccess = { unlockSuccess = true },
                onDismiss = {}
            )
        }

        // Enter correct pin "1234"
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        // Submit
        composeTestRule.onNodeWithTag(PIN_SUBMIT_TEST_TAG).performClick()

        assertTrue(unlockSuccess)
    }
}
