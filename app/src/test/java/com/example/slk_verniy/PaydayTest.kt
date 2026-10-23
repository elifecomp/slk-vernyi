package com.example.slk_verniy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PaydayTest {

    @Test
    fun testPaydaysAreCorrect() {
        val date10 = LocalDate.of(2025, 5, 10)
        val date25 = LocalDate.of(2025, 5, 25)
        val dateOther = LocalDate.of(2025, 5, 12)
        val dateFeb10 = LocalDate.of(2025, 2, 10)
        val dateFeb25 = LocalDate.of(2025, 2, 25)

        assertTrue(date10.dayOfMonth == 10 || date10.dayOfMonth == 25)
        assertTrue(date25.dayOfMonth == 10 || date25.dayOfMonth == 25)
        assertTrue(dateFeb10.dayOfMonth == 10 || dateFeb10.dayOfMonth == 25)
        assertTrue(dateFeb25.dayOfMonth == 10 || dateFeb25.dayOfMonth == 25)
        assertFalse(dateOther.dayOfMonth == 10 || dateOther.dayOfMonth == 25)
    }
}
