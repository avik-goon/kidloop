package com.kidloop.app.timer

import com.kidloop.app.util.Clock
import com.kidloop.app.util.SystemClock
import java.time.Instant
import java.time.ZoneId

data class TimerState(
    val sessionRemainingSeconds: Long,
    val sessionLimitSeconds: Long,
    val dailyRemainingSeconds: Long,
    val dailyLimitSeconds: Long,
    val isSessionExpired: Boolean,
    val isDailyExpired: Boolean
) {
    val isAnyLimitReached: Boolean get() = isSessionExpired || isDailyExpired
}

class TimeLimitManager(
    private val clock: Clock = SystemClock(),
    private var sessionLimitMinutes: Int = 20,
    private var dailyLimitMinutes: Int = 60,
    initialDailyUsedSeconds: Long = 0L,
    initialDateString: String = "",
    private val onPersistUsage: (suspend (dateString: String, usedSeconds: Long) -> Unit)? = null
) {
    private var sessionUsedSeconds = 0L
    private var dailyUsedSeconds = initialDailyUsedSeconds
    private var currentDateString = initialDateString
    private var secondsSinceLastPersist = 0

    init {
        checkAndHandleDateRollover()
    }

    fun getLocalDateString(): String {
        return Instant.ofEpochMilli(clock.currentTimeMillis())
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString() // "yyyy-MM-dd"
    }

    /**
     * Checks if local date has advanced to a new day; if so, resets daily counter.
     */
    fun checkAndHandleDateRollover(): Boolean {
        val today = getLocalDateString()
        if (currentDateString.isNotBlank() && currentDateString != today) {
            dailyUsedSeconds = 0L
            currentDateString = today
            secondsSinceLastPersist = 0
            return true
        }
        if (currentDateString.isBlank()) {
            currentDateString = today
        }
        return false
    }

    /**
     * Advances playback duration by one second if video is playing.
     * Checks session limit, daily limit, date rollover, and triggers 15-second persistence.
     */
    suspend fun tickPlaybackSecond(): TimerState {
        checkAndHandleDateRollover()

        sessionUsedSeconds++
        dailyUsedSeconds++
        secondsSinceLastPersist++

        if (secondsSinceLastPersist >= 15) {
            secondsSinceLastPersist = 0
            onPersistUsage?.invoke(currentDateString, dailyUsedSeconds)
        }

        return getTimerState()
    }

    fun getTimerState(): TimerState {
        checkAndHandleDateRollover()

        val sessionLimitSeconds = (sessionLimitMinutes * 60L).coerceAtLeast(60L)
        val dailyLimitSeconds = (dailyLimitMinutes * 60L).coerceAtLeast(60L)

        val sessionRemaining = (sessionLimitSeconds - sessionUsedSeconds).coerceAtLeast(0L)
        val dailyRemaining = (dailyLimitSeconds - dailyUsedSeconds).coerceAtLeast(0L)

        val isSessionExpired = sessionUsedSeconds >= sessionLimitSeconds
        val isDailyExpired = dailyUsedSeconds >= dailyLimitSeconds

        return TimerState(
            sessionRemainingSeconds = sessionRemaining,
            sessionLimitSeconds = sessionLimitSeconds,
            dailyRemainingSeconds = dailyRemaining,
            dailyLimitSeconds = dailyLimitSeconds,
            isSessionExpired = isSessionExpired,
            isDailyExpired = isDailyExpired
        )
    }

    fun isDailyLimitExhausted(): Boolean {
        checkAndHandleDateRollover()
        val dailyLimitSeconds = (dailyLimitMinutes * 60L).coerceAtLeast(60L)
        return dailyUsedSeconds >= dailyLimitSeconds
    }

    fun resetSession() {
        sessionUsedSeconds = 0L
    }

    suspend fun resetTodayUsage() {
        dailyUsedSeconds = 0L
        currentDateString = getLocalDateString()
        secondsSinceLastPersist = 0
        onPersistUsage?.invoke(currentDateString, 0L)
    }

    fun updateLimits(sessionMinutes: Int, dailyMinutes: Int) {
        sessionLimitMinutes = sessionMinutes
        dailyLimitMinutes = dailyMinutes
    }

    suspend fun forcePersist() {
        onPersistUsage?.invoke(currentDateString, dailyUsedSeconds)
    }

    fun getDailyUsedSeconds(): Long = dailyUsedSeconds
    fun getSessionUsedSeconds(): Long = sessionUsedSeconds
}
