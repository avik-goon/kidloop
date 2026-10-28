package com.kidloop.app.player

import org.junit.Assert.assertEquals
import org.junit.Test

class CircularQueueTest {

    private fun nextIndex(currentIndex: Int, size: Int): Int {
        if (size <= 0) return 0
        return (currentIndex + 1) % size
    }

    private fun previousIndex(currentIndex: Int, size: Int): Int {
        if (size <= 0) return 0
        return (currentIndex - 1 + size) % size
    }

    @Test
    fun `next index advances linearly in bounds`() {
        assertEquals(1, nextIndex(0, 3))
        assertEquals(2, nextIndex(1, 3))
    }

    @Test
    fun `next index wraps around from last to first`() {
        assertEquals(0, nextIndex(2, 3))
    }

    @Test
    fun `previous index wraps around from first to last`() {
        assertEquals(2, previousIndex(0, 3))
    }

    @Test
    fun `previous index moves backwards in bounds`() {
        assertEquals(1, previousIndex(2, 3))
        assertEquals(0, previousIndex(1, 3))
    }

    @Test
    fun `single item queue remains at 0 for both next and previous`() {
        assertEquals(0, nextIndex(0, 1))
        assertEquals(0, previousIndex(0, 1))
    }

    @Test
    fun `empty queue returns 0 without crashing`() {
        assertEquals(0, nextIndex(0, 0))
        assertEquals(0, previousIndex(0, 0))
    }
}
