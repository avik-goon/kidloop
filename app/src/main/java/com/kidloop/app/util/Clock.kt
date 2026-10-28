package com.kidloop.app.util

/**
 * Abstraction for system time to allow deterministic unit testing of
 * timers, date rollovers, and timeouts.
 */
interface Clock {
    fun currentTimeMillis(): Long
}

class SystemClock : Clock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
