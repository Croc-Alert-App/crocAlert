package crocalert.app.ui.auth

import crocalert.app.UnitTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@UnitTest
class SessionManagerTest {

    @BeforeTest
    fun setup() {
        // Ensure clean state before each test since SessionManager is a singleton
        SessionManager.forgetDevice()
    }

    @AfterTest
    fun tearDown() {
        SessionManager.forgetDevice()
    }

    @Test
    fun `isDeviceRemembered is false by default`() {
        assertFalse(SessionManager.isDeviceRemembered)
    }

    @Test
    fun `rememberDevice sets isDeviceRemembered to true`() {
        SessionManager.rememberDevice()
        assertTrue(SessionManager.isDeviceRemembered)
    }

    @Test
    fun `forgetDevice sets isDeviceRemembered to false`() {
        SessionManager.rememberDevice()
        SessionManager.forgetDevice()
        assertFalse(SessionManager.isDeviceRemembered)
    }

    @Test
    fun `calling rememberDevice multiple times keeps state true`() {
        SessionManager.rememberDevice()
        SessionManager.rememberDevice()
        assertTrue(SessionManager.isDeviceRemembered)
    }

    @Test
    fun `calling forgetDevice when already forgotten stays false`() {
        SessionManager.forgetDevice()
        SessionManager.forgetDevice()
        assertFalse(SessionManager.isDeviceRemembered)
    }

    @Test
    fun `remember then forget then remember restores true`() {
        SessionManager.rememberDevice()
        SessionManager.forgetDevice()
        SessionManager.rememberDevice()
        assertTrue(SessionManager.isDeviceRemembered)
    }
}
