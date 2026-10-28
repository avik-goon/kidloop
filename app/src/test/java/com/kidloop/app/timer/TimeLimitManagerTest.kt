package com.kidloop.app.timer

import com.kidloop.app.lock.FakeClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class TimeLimitManagerTest {

    private lateinit var fakeClock: FakeClock
    private var persistedDate: String? = null
    private var persistedSeconds: Long? = null

    @Before
    fun setup() {
        // Set fixed start time: 2026-10-28 10:00:00 UTC (1793268000000L)
        fakeClock = FakeClock(1793268000000L)
        persistedDate = null
        persistedSeconds = null
    }

    private fun createManager(
        sessionLimit: Int = 20,
        dailyLimit: Int = 60,
        initialDailyUsed: Long = 0L,
        initialDate: String = ""
    ): TimeLimitManager {
        return TimeLimitManager(
            clock = fakeClock,
            sessionLimitMinutes = sessionLimit,
            dailyLimitMinutes = dailyLimit,
            initialDailyUsedSeconds = initialDailyUsed,
            initialDateString = initialDate,
            onPersistUsage = { date, seconds ->
                persistedDate = date
                persistedSeconds = seconds
            }
        )
    }

    @Test
    fun `initial state has full session and daily limits remaining`() {
        val manager = createManager(sessionLimit = 20, dailyLimit = 60)
        val state = manager.getTimerState()

        assertEquals(1200L, state.sessionLimitSeconds)
        assertEquals(1200L, state.sessionRemainingSeconds)
        assertEquals(3600L, state.dailyLimitSeconds)
        assertEquals(3600L, state.dailyRemainingSeconds)
        assertFalse(state.isSessionExpired)
        assertFalse(state.isDailyExpired)
    }

    @Test
    fun `playback ticks advance session and daily time`() = runTest {
        val manager = createManager(sessionLimit = 20, dailyLimit = 60)

        repeat(10) {
            manager.tickPlaybackSecond()
        }

        val state = manager.getTimerState()
        assertEquals(1190L, state.sessionRemainingSeconds)
        assertEquals(3590L, state.dailyRemainingSeconds)
    }

    @Test
    fun `session limit expires after reaching limit seconds`() = runTest {
        // 1 minute session limit = 60 seconds
        val manager = createManager(sessionLimit = 1, dailyLimit = 60)

        repeat(59) {
            val state = manager.tickPlaybackSecond()
            assertFalse(state.isSessionExpired)
        }

        val expiredState = manager.tickPlaybackSecond()
        assertTrue(expiredState.isSessionExpired)
        assertFalse(expiredState.isDailyExpired)
        assertEquals(0L, expiredState.sessionRemainingSeconds)
    }

    @Test
    fun `reset session clears session usage but preserves daily usage`() = runTest {
        val manager = createManager(sessionLimit = 10, dailyLimit = 60)

        repeat(100) {
            manager.tickPlaybackSecond()
        }

        assertEquals(100L, manager.getSessionUsedSeconds())
        assertEquals(100L, manager.getDailyUsedSeconds())

        manager.resetSession()
        assertEquals(0L, manager.getSessionUsedSeconds())
        assertEquals(100L, manager.getDailyUsedSeconds())
    }

    @Test
    fun `persistence callback is triggered every 15 seconds`() = runTest {
        val manager = createManager(sessionLimit = 20, dailyLimit = 60)

        repeat(14) {
            manager.tickPlaybackSecond()
        }
        assertEquals(null, persistedSeconds)

        // 15th tick triggers persist
        manager.tickPlaybackSecond()
        assertEquals(15L, persistedSeconds)

        repeat(14) {
            manager.tickPlaybackSecond()
        }
        assertEquals(15L, persistedSeconds)

        // 30th tick triggers next persist
        manager.tickPlaybackSecond()
        assertEquals(30L, persistedSeconds)
    }

    @Test
    fun `local midnight rollover automatically resets daily usage`() = runTest {
        val manager = createManager(sessionLimit = 20, dailyLimit = 60)

        repeat(30) {
            manager.tickPlaybackSecond()
        }
        assertEquals(30L, manager.getDailyUsedSeconds())

        // Advance fake clock by 24 hours (86,400 seconds) across midnight
        fakeClock.advanceSeconds(86400L)

        // Next tick triggers date rollover detection
        manager.tickPlaybackSecond()
        assertEquals(1L, manager.getDailyUsedSeconds())
    }

    @Test
    fun `recovery from persistent storage restores previous daily usage`() {
        val todayStr = Instant.ofEpochMilli(fakeClock.currentTimeMillis())
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()

        val manager = createManager(
            sessionLimit = 20,
            dailyLimit = 60,
            initialDailyUsed = 1800L,
            initialDate = todayStr
        )

        val state = manager.getTimerState()
        assertEquals(1800L, state.dailyRemainingSeconds)
        assertFalse(state.isDailyExpired)
    }
}
