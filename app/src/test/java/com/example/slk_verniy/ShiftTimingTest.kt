package com.example.slk_verniy

import com.example.slk_verniy.worker.isWorkDay
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ShiftTimingTest {

    @Test
    fun testIsWorkDaySchedule() {
        val startDate = LocalDate.of(2025, 1, 1) // Day 0 (Work)
        val day1 = LocalDate.of(2025, 1, 2)      // Day 1 (Work)
        val day2 = LocalDate.of(2025, 1, 3)      // Day 2 (Rest)
        val day3 = LocalDate.of(2025, 1, 4)      // Day 3 (Rest)
        val day4 = LocalDate.of(2025, 1, 5)      // Day 4 (Work)
        val beforeStart = LocalDate.of(2024, 12, 31)

        assertTrue(isWorkDay(startDate, startDate, emptySet()))
        assertTrue(isWorkDay(day1, startDate, emptySet()))
        assertFalse(isWorkDay(day2, startDate, emptySet()))
        assertFalse(isWorkDay(day3, startDate, emptySet()))
        assertTrue(isWorkDay(day4, startDate, emptySet()))
        assertFalse(isWorkDay(beforeStart, startDate, emptySet()))
        assertFalse(isWorkDay(day2, null, emptySet()))
    }

    @Test
    fun testExtraShiftsOverride() {
        val startDate = LocalDate.of(2025, 1, 1) // Day 2 (Rest)
        val restDay = LocalDate.of(2025, 1, 3)   // Normally rest day

        assertFalse(isWorkDay(restDay, startDate, emptySet()))
        // With extra shift added
        assertTrue(isWorkDay(restDay, startDate, setOf(restDay)))
    }
}
