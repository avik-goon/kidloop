package com.kidloop.app.lock

import com.kidloop.app.util.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeClock(var currentMillis: Long = 1_000_000L) : Clock {
    override fun currentTimeMillis(): Long = currentMillis

    fun advanceSeconds(seconds: Long) {
        currentMillis += (seconds * 1000L)
    }
}

class PinLockoutManagerTest {

    private lateinit var fakeClock: FakeClock
    private lateinit var lockoutManager: PinLockoutManager

    @Before
    fun setup() {
        fakeClock = FakeClock()
        lockoutManager = PinLockoutManager(
            clock = fakeClock,
            maxAttempts = 5,
            lockoutDurationSeconds = 60L,
            inactivityTimeoutSeconds = 15L
        )
    }

    @Test
    fun `initial state allows 5 attempts and is not locked out`() {
        assertFalse(lockoutManager.isLockedOut())
        assertEquals(5, lockoutManager.remainingAttempts())
        assertEquals(0, lockoutManager.lockoutSecondsRemaining())
    }

    @Test
    fun `failed attempts decrement remaining attempts`() {
        lockoutManager.recordAttempt(isCorrect = false)
        assertEquals(4, lockoutManager.remainingAttempts())

        lockoutManager.recordAttempt(isCorrect = false)
        assertEquals(3, lockoutManager.remainingAttempts())
    }

    @Test
    fun `five failed attempts trigger 60 second lockout`() {
        repeat(5) {
            lockoutManager.recordAttempt(isCorrect = false)
        }

        assertTrue(lockoutManager.isLockedOut())
        assertEquals(0, lockoutManager.remainingAttempts())
        assertEquals(60, lockoutManager.lockoutSecondsRemaining())

        // Advancing clock by 30 seconds
        fakeClock.advanceSeconds(30)
        assertTrue(lockoutManager.isLockedOut())
        assertEquals(30, lockoutManager.lockoutSecondsRemaining())

        // Advancing clock beyond 60 seconds clears lockout
        fakeClock.advanceSeconds(31)
        assertFalse(lockoutManager.isLockedOut())
        assertEquals(5, lockoutManager.remainingAttempts())
    }

    @Test
    fun `correct attempt resets failed attempts count`() {
        lockoutManager.recordAttempt(isCorrect = false)
        lockoutManager.recordAttempt(isCorrect = false)
        assertEquals(3, lockoutManager.remainingAttempts())

        val success = lockoutManager.recordAttempt(isCorrect = true)
        assertTrue(success)
        assertEquals(5, lockoutManager.remainingAttempts())
        assertFalse(lockoutManager.isLockedOut())
    }

    @Test
    fun `inactivity timeout triggers after 15 seconds without user activity`() {
        assertFalse(lockoutManager.isInactivityTimedOut())
        assertEquals(15, lockoutManager.inactivitySecondsRemaining())

        fakeClock.advanceSeconds(10)
        assertFalse(lockoutManager.isInactivityTimedOut())
        assertEquals(5, lockoutManager.inactivitySecondsRemaining())

        // User interacts (taps digit), resetting inactivity timer
        lockoutManager.recordActivity()
        assertEquals(15, lockoutManager.inactivitySecondsRemaining())

        fakeClock.advanceSeconds(15)
        assertTrue(lockoutManager.isInactivityTimedOut())
    }
}
