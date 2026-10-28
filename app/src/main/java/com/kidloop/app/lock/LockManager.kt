package com.kidloop.app.lock

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object LockManager {

    /**
     * Starts Android Screen Pinning (Lock Task Mode).
     */
    fun startLockTask(activity: Activity) {
        try {
            val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                activity.startLockTask()
            }
        } catch (_: Exception) {
            // Handled gracefully if pinning request is rejected by system policy
        }
    }

    /**
     * Stops Android Screen Pinning if currently active.
     */
    fun stopLockTask(activity: Activity) {
        try {
            val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
                activity.stopLockTask()
            }
        } catch (_: Exception) {
            // Handled gracefully
        }
    }

    /**
     * Checks if Screen Pinning is actively engaged.
     */
    fun isScreenPinned(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    /**
     * Configures sticky immersive fullscreen mode hiding status and navigation bars.
     */
    fun applyImmersiveMode(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    /**
     * Restores system bars when leaving Kid Mode.
     */
    fun restoreSystemBars(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    /**
     * Controls FLAG_KEEP_SCREEN_ON.
     */
    fun setKeepScreenOn(window: Window, keepOn: Boolean) {
        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * Enables FLAG_SECURE to prevent screenshots and recent apps switcher thumbnail previews.
     */
    fun setSecureFlag(window: Window, secure: Boolean) {
        if (secure) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
