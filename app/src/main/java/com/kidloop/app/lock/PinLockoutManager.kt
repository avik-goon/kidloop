package com.kidloop.app.lock

import com.kidloop.app.util.Clock
import com.kidloop.app.util.SystemClock

class PinLockoutManager(
    private val clock: Clock = SystemClock(),
    private val maxAttempts: Int = 5,
    private val lockoutDurationSeconds: Long = 60L,
    private val inactivityTimeoutSeconds: Long = 15L
) {
    private var failedAttempts = 0
    private var lockoutUntilTimestamp = 0L
    private var lastActivityTimestamp = clock.currentTimeMillis()

    /**
     * Records a PIN verification attempt.
     * Returns true if the attempt was successful.
     * If failed, increments failed attempts. If attempts reach [maxAttempts],
     * triggers lockout for [lockoutDurationSeconds].
     */
    fun recordAttempt(isCorrect: Boolean): Boolean {
        recordActivity()
        if (isLockedOut()) {
            return false
        }

        if (isCorrect) {
            reset()
            return true
        } else {
            failedAttempts++
            if (failedAttempts >= maxAttempts) {
                lockoutUntilTimestamp = clock.currentTimeMillis() + (lockoutDurationSeconds * 1000L)
            }
            return false
        }
    }

    /**
     * Checks if PIN entry is currently locked out due to excessive failed attempts.
     */
    fun isLockedOut(): Boolean {
        val now = clock.currentTimeMillis()
        if (lockoutUntilTimestamp > now) {
            return true
        }
        if (lockoutUntilTimestamp != 0L && now >= lockoutUntilTimestamp) {
            // Lockout period expired; reset failed attempts count
            failedAttempts = 0
            lockoutUntilTimestamp = 0L
        }
        return false
    }

    /**
     * Returns the remaining attempts before lockout.
     */
    fun remainingAttempts(): Int {
        if (isLockedOut()) return 0
        return (maxAttempts - failedAttempts).coerceAtLeast(0)
    }

    /**
     * Returns the seconds remaining until the lockout expires.
     */
    fun lockoutSecondsRemaining(): Int {
        val now = clock.currentTimeMillis()
        val diffMs = lockoutUntilTimestamp - now
        return if (diffMs > 0) ((diffMs + 999) / 1000).toInt() else 0
    }

    /**
     * Records interaction to reset the 15-second auto-dismiss inactivity timer.
     */
    fun recordActivity() {
        lastActivityTimestamp = clock.currentTimeMillis()
    }

    /**
     * Checks if 15 seconds of inactivity have elapsed since the last user interaction.
     */
    fun isInactivityTimedOut(): Boolean {
        val elapsedMs = clock.currentTimeMillis() - lastActivityTimestamp
        return elapsedMs >= (inactivityTimeoutSeconds * 1000L)
    }

    /**
     * Returns seconds remaining before auto-dismissal due to inactivity.
     */
    fun inactivitySecondsRemaining(): Int {
        val elapsedMs = clock.currentTimeMillis() - lastActivityTimestamp
        val remainingMs = (inactivityTimeoutSeconds * 1000L) - elapsedMs
        return if (remainingMs > 0) ((remainingMs + 999) / 1000).toInt() else 0
    }

    /**
     * Resets failed attempts, lockout state, and activity timer.
     */
    fun reset() {
        failedAttempts = 0
        lockoutUntilTimestamp = 0L
        lastActivityTimestamp = clock.currentTimeMillis()
    }
}
