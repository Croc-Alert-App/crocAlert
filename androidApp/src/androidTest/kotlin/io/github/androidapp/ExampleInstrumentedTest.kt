package io.github.androidapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumented tests for [MainActivity].
 *
 * These run on a real device or emulator (not included in the standard JVM CI job).
 * They verify that the app launches correctly and the root Compose content is rendered.
 *
 * To run locally:
 *   ./gradlew :androidApp:connectedAndroidTest
 *
 * Note: Koin DI is initialised by the Application/MainActivity. If Koin startup
 * fails (e.g. missing modules), these tests will catch the crash immediately.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ── Package sanity ────────────────────────────────────────────────────────

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.github.androidapp", appContext.packageName)
    }

    // ── Launch smoke tests ────────────────────────────────────────────────────

    /**
     * Verifies the root Compose node is rendered after MainActivity starts.
     * A failure here means the app crashes on launch or Koin DI fails to initialise.
     */
    @Test
    fun mainActivity_launches_and_renders_root_composable() {
        composeTestRule.waitForIdle()
        // Root composable must be present — if the app crashed, this assertion fails.
        composeTestRule.onRoot().assertIsDisplayed()
    }

    /**
     * Verifies the activity window is in the expected state after launch.
     * Uses idle waiting so flaky animation timing is not an issue.
     */
    @Test
    fun mainActivity_is_not_finishing_after_launch() {
        composeTestRule.waitForIdle()
        val activity = composeTestRule.activity
        assertEquals(
            "Activity should not be finishing after launch",
            false,
            activity.isFinishing
        )
    }
}
