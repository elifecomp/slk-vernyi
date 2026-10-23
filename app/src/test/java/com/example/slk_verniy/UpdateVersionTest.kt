package com.example.slk_verniy

import com.example.slk_verniy.worker.UpdateManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionTest {

    @Test
    fun testIsNewerVersion() {
        // Semver and tag prefix tests
        assertFalse(UpdateManager.isNewerVersion("1.1.3", "v1.1.3"))
        assertFalse(UpdateManager.isNewerVersion("1.1.3", "1.1.3"))
        assertTrue(UpdateManager.isNewerVersion("1.1.3", "1.1.4"))
        assertTrue(UpdateManager.isNewerVersion("1.1.3", "v1.1.4"))
        assertTrue(UpdateManager.isNewerVersion("1.1.0", "1.1.1"))
        assertTrue(UpdateManager.isNewerVersion("1.1.0", "1.2.0"))
        assertTrue(UpdateManager.isNewerVersion("1.1.0", "2.0.0"))
        assertTrue(UpdateManager.isNewerVersion("1.1.0", "v1.1.1"))
        assertFalse(UpdateManager.isNewerVersion("1.1.0", "1.1.0"))
        assertFalse(UpdateManager.isNewerVersion("1.1.1", "1.1.0"))
        assertFalse(UpdateManager.isNewerVersion("1.2.0", "1.1.0"))
    }
}
