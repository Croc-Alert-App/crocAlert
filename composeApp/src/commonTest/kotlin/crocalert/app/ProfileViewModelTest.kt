package crocalert.app

import crocalert.app.shared.sync.InMemorySyncPreferencesProvider
import crocalert.app.ui.profile.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm(provider: InMemorySyncPreferencesProvider = InMemorySyncPreferencesProvider()) =
        ProfileViewModel(syncPrefsProvider = provider)

    @Test
    fun `setAlertsTtl(0) coerces to 1`() = runTest {
        val provider = InMemorySyncPreferencesProvider()
        val v = vm(provider)
        v.setAlertsTtl(0)
        advanceUntilIdle()
        assertEquals(1, v.syncPreferences.value.alertsTtlMinutes)
    }

    @Test
    fun `setAlertsTtl(9999) coerces to 1440`() = runTest {
        val provider = InMemorySyncPreferencesProvider()
        val v = vm(provider)
        v.setAlertsTtl(9999)
        advanceUntilIdle()
        assertEquals(1440, v.syncPreferences.value.alertsTtlMinutes)
    }

    @Test
    fun `setCamerasTtl(0) coerces to 1`() = runTest {
        val provider = InMemorySyncPreferencesProvider()
        val v = vm(provider)
        v.setCamerasTtl(0)
        advanceUntilIdle()
        assertEquals(1, v.syncPreferences.value.camerasTtlMinutes)
    }

    @Test
    fun `setCamerasTtl(9999) coerces to 1440`() = runTest {
        val provider = InMemorySyncPreferencesProvider()
        val v = vm(provider)
        v.setCamerasTtl(9999)
        advanceUntilIdle()
        assertEquals(1440, v.syncPreferences.value.camerasTtlMinutes)
    }

}
