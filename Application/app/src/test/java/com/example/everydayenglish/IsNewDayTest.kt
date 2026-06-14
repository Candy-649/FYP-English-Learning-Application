package com.example.everydayenglish

import org.junit.Assert.*
import org.junit.Test

class IsNewDayTest {
    private fun isNewDay(lastMs: Long): Boolean {
        if (lastMs == 0L) return true
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return lastMs < cal.timeInMillis
    }

    @Test
    fun `lastMs zero returns true legacy data`() {
        assertTrue(isNewDay(0L))
    }

    @Test
    fun `yesterday returns true`() {
        val yesterday = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        assertTrue(isNewDay(yesterday))
    }

    @Test
    fun `just now returns false`() {
        val justNow = System.currentTimeMillis()
        assertFalse(isNewDay(justNow))
    }

    @Test
    fun `future timestamp returns false`() {
        val future = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
        assertFalse(isNewDay(future))
    }
}