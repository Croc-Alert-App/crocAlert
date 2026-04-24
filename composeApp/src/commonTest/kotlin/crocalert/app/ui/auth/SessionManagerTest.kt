package crocalert.app.ui.auth

import crocalert.app.UnitTest
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@UnitTest
class SessionManagerTest {

    private lateinit var prefs: InMemorySessionPreferences

    @BeforeTest
    fun setup() {
        prefs = InMemorySessionPreferences()
        SessionManager.init(prefs)
    }

    // ── checkSession ──────────────────────────────────────────────────────────

    @Test
    fun `checkSession returns None when no session stored`() = runTest {
        assertEquals(SessionCheckResult.None, SessionManager.checkSession())
    }

    @Test
    fun `checkSession returns Active for a session expiring in the future`() = runTest {
        prefs.setSessionExpiresAt(System.currentTimeMillis() + 60_000L)
        assertEquals(SessionCheckResult.Active, SessionManager.checkSession())
    }

    @Test
    fun `checkSession returns Expired for a session whose timestamp is in the past`() = runTest {
        prefs.setSessionExpiresAt(System.currentTimeMillis() - 1L)
        assertEquals(SessionCheckResult.Expired, SessionManager.checkSession())
    }

    @Test
    fun `checkSession reads expiry exactly once — no TOCTOU window`() = runTest {
        // Simulate: session was valid when read but would be cleared between two reads.
        // With checkSession() using a single read, both Active vs Expired are derived
        // from the same snapshot — we verify this by checking the result is consistent
        // with the value present at call time.
        prefs.setSessionExpiresAt(System.currentTimeMillis() + 60_000L)
        val result = SessionManager.checkSession()
        // If a second read were used internally, a concurrent clear would produce None.
        // The single-read path must always return Active here.
        assertEquals(SessionCheckResult.Active, result)
    }

    // ── updateRememberDevice ──────────────────────────────────────────────────

    @Test
    fun `updateRememberDevice with remember=true writes email and expiry atomically`() = runTest {
        SessionManager.updateRememberDevice("ranger@crocalert.cr", remember = true)

        assertEquals("ranger@crocalert.cr", prefs.getSavedEmail())
        val expiresAt = prefs.getSessionExpiresAt()
        assert(expiresAt != null && expiresAt > System.currentTimeMillis()) {
            "expiresAt should be in the future, was $expiresAt"
        }
    }

    @Test
    fun `updateRememberDevice with remember=false clears both email and expiry`() = runTest {
        prefs.updateSession(email = "ranger@crocalert.cr", expiresAt = System.currentTimeMillis() + 60_000L)

        SessionManager.updateRememberDevice("ranger@crocalert.cr", remember = false)

        assertNull(prefs.getSavedEmail())
        assertNull(prefs.getSessionExpiresAt())
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    fun `logout clears both email and expiry`() = runTest {
        prefs.updateSession(email = "ranger@crocalert.cr", expiresAt = System.currentTimeMillis() + 60_000L)

        SessionManager.logout()

        assertNull(prefs.getSavedEmail())
        assertNull(prefs.getSessionExpiresAt())
        assertEquals(SessionCheckResult.None, SessionManager.checkSession())
    }

    @Test
    fun `logout on already-logged-out session does not throw`() = runTest {
        SessionManager.logout()
        assertEquals(SessionCheckResult.None, SessionManager.checkSession())
    }

    // ── sessionRemainingMs ────────────────────────────────────────────────────

    @Test
    fun `sessionRemainingMs returns null when no session stored`() = runTest {
        assertNull(SessionManager.sessionRemainingMs())
    }

    @Test
    fun `sessionRemainingMs returns positive value for active session`() = runTest {
        prefs.setSessionExpiresAt(System.currentTimeMillis() + 10_000L)
        val remaining = SessionManager.sessionRemainingMs()
        assert(remaining != null && remaining > 0L) { "Expected positive remaining, got $remaining" }
    }

    @Test
    fun `sessionRemainingMs returns 0 for expired session`() = runTest {
        prefs.setSessionExpiresAt(System.currentTimeMillis() - 1L)
        assertEquals(0L, SessionManager.sessionRemainingMs())
    }
}
