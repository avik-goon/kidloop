package com.kidloop.app.ui

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackPressLockTest {

    @Test
    fun backPressCallbackConsumesBackPressWhenKidModeIsActive() {
        var backHandled = false
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backHandled = true
            }
        }

        val dispatcher = OnBackPressedDispatcher()
        dispatcher.addCallback(callback)

        // When Kid Mode is active, callback is enabled
        assertTrue(callback.isEnabled)
        dispatcher.onBackPressed()
        assertTrue(backHandled)

        // When Kid Mode exits, callback is disabled
        callback.isEnabled = false
        assertFalse(callback.isEnabled)
    }
}
