package crocalert.app

import crocalert.app.shared.sync.SyncPreferences
import kotlin.test.*

class SyncPreferencesTest {

    @Test
    fun `default alertsTtlMinutes is 5`() {
        assertEquals(5, SyncPreferences().alertsTtlMinutes)
    }

    @Test
    fun `default camerasTtlMinutes is 15`() {
        assertEquals(15, SyncPreferences().camerasTtlMinutes)
    }

    @Test
    fun `default sitesTtlMinutes is 60`() {
        assertEquals(60, SyncPreferences().sitesTtlMinutes)
    }

    @Test
    fun `custom values are stored correctly`() {
        val prefs = SyncPreferences(alertsTtlMinutes = 1, camerasTtlMinutes = 2, sitesTtlMinutes = 3)
        assertEquals(1, prefs.alertsTtlMinutes)
        assertEquals(2, prefs.camerasTtlMinutes)
        assertEquals(3, prefs.sitesTtlMinutes)
    }

    @Test
    fun `data class equality holds for same values`() {
        assertEquals(SyncPreferences(), SyncPreferences())
    }

    @Test
    fun `copy produces distinct instance with updated field`() {
        val original = SyncPreferences()
        val modified = original.copy(alertsTtlMinutes = 10)
        assertEquals(10, modified.alertsTtlMinutes)
        assertEquals(original.camerasTtlMinutes, modified.camerasTtlMinutes)
        assertEquals(original.sitesTtlMinutes, modified.sitesTtlMinutes)
        assertNotEquals(original, modified)
    }
}
