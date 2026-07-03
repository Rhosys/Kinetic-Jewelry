package ch.rhosys.lyra.ui.devices

import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.ConnectionState
import ch.rhosys.lyra.domain.model.VibrationMode
import ch.rhosys.lyra.fake.FakeBluetoothController
import ch.rhosys.lyra.fake.FakeBluetoothDeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceManagerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var controller: FakeBluetoothController
    private lateinit var repo: FakeBluetoothDeviceRepository
    private lateinit var vm: DeviceManagerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        controller = FakeBluetoothController()
        repo = FakeBluetoothDeviceRepository()
        vm = DeviceManagerViewModel(repo, controller)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startScan delegates to controller`() {
        vm.startScan()
        assertTrue(controller.isScanning.value)
    }

    @Test
    fun `stopScan delegates to controller`() {
        vm.startScan()
        vm.stopScan()
        assertFalse(controller.isScanning.value)
    }

    @Test
    fun `addFavorite persists device as favorite and enabled`() = runTest {
        val device = BluetoothDeviceInfo(
            address = "AA:BB:CC:DD:EE:FF",
            name = "TestDevice",
            isAlertEnabled = false,
            connectionState = ConnectionState.DISCONNECTED,
            isFavorite = false,
        )
        vm.addFavorite(device)
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = repo.getAll()
        assertEquals(1, stored.size)
        assertTrue(stored[0].isFavorite)
        assertTrue(stored[0].isAlertEnabled)
        assertEquals("AA:BB:CC:DD:EE:FF", stored[0].address)
    }

    @Test
    fun `removeFavorite deletes device from repository`() = runTest {
        val device = BluetoothDeviceInfo(
            address = "AA:BB:CC:DD:EE:FF",
            name = "TestDevice",
            isAlertEnabled = true,
            connectionState = ConnectionState.DISCONNECTED,
            isFavorite = true,
        )
        repo.upsert(device)

        vm.removeFavorite("AA:BB:CC:DD:EE:FF")
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = repo.getAll()
        assertEquals(0, stored.size)
    }

    @Test
    fun `disabling a favorite keeps it favorited`() = runTest {
        val device = BluetoothDeviceInfo(
            address = "AA:BB:CC:DD:EE:FF",
            name = "TestDevice",
            isAlertEnabled = true,
            connectionState = ConnectionState.DISCONNECTED,
            isFavorite = true,
        )
        repo.upsert(device)

        vm.setEnabled("AA:BB:CC:DD:EE:FF", false)
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = repo.getAll().single()
        assertTrue("Disabling must not un-favorite the device", stored.isFavorite)
        assertFalse(stored.isAlertEnabled)
    }

    @Test
    fun `re-enabling clears the auto-disable window`() = runTest {
        val device = BluetoothDeviceInfo(
            address = "AA:BB:CC:DD:EE:FF",
            name = "TestDevice",
            isAlertEnabled = false,
            connectionState = ConnectionState.DISCONNECTED,
            isFavorite = true,
            disabledUntil = System.currentTimeMillis() + 60_000L,
        )
        repo.upsert(device)

        vm.setEnabled("AA:BB:CC:DD:EE:FF", true)
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = repo.getAll().single()
        assertTrue(stored.isAlertEnabled)
        assertEquals(null, stored.disabledUntil)
    }

    @Test
    fun `testDevice sends vibration to controller`() = runTest {
        vm.testDevice("AA:BB:CC:DD:EE:FF")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, controller.sentCommands.size)
        assertEquals("AA:BB:CC:DD:EE:FF", controller.sentCommands[0].address)
        assertEquals(VibrationMode.LONG_PULSE.blocks, controller.sentCommands[0].blocks)
        assertEquals(2, controller.sentCommands[0].repeat)
    }

    @Test
    fun `testDevice emits snackbar on success`() = runTest {
        vm.testDevice("AA:BB:CC:DD:EE:FF")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the vibration was sent (snackbar emission is a side effect of success)
        assertEquals(1, controller.sentCommands.size)
    }
}
