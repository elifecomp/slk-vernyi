package com.example.slk_verniy

import com.example.slk_verniy.worker.AppUpdateChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionTest {

    @Test
    fun testIsNewerVersion() {
        assertTrue(AppUpdateChecker.isNewerVersion("1.1.0", "1.1.1"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.1.0", "1.2.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.1.0", "2.0.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.1.0", "v1.1.1"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.1.0", "1.1.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.1.1", "1.1.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.2.0", "1.1.0"))
    }
}
