package com.example.slk_verniy

import org.junit.Test
import java.time.LocalDate

class DataStoreTest {
    @Test
    fun testLocalDate() {
        val date = LocalDate.now()
        val str = date.toString()
        val parsed = LocalDate.parse(str)
        assert(date == parsed)
    }

    @Test
    fun testExtraShiftsSetSerialization() {
        val dates = setOf(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 1, 20))
        val serialized = dates.map { it.toString() }.toSet()
        val deserialized = serialized.map { LocalDate.parse(it) }.toSet()
        assert(dates == deserialized)
    }
}
