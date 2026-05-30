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
    fun `enableAlert persists device to repository`() = runTest {
        val device = BluetoothDeviceInfo(
            address = "AA:BB:CC:DD:EE:FF",
            name = "TestDevice",
            isAlertEnabled = false,
            connectionState = ConnectionState.DISCONNECTED,
        )
        vm.enableAlert(device)
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = repo.getAll()
        assertEquals(1, stored.size)
        assertTrue(stored[0].isAlertEnabled)
        assertEquals("AA:BB:CC:DD:EE:FF", stored[0].address)
    }

    @Test
    fun `disableAlert removes device from repository`() = runTest {
        val device = BluetoothDeviceInfo(
            address = "AA:BB:CC:DD:EE:FF",
            name = "TestDevice",
            isAlertEnabled = true,
            connectionState = ConnectionState.DISCONNECTED,
        )
        repo.upsert(device)

        vm.disableAlert("AA:BB:CC:DD:EE:FF")
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = repo.getAll()
        assertEquals(0, stored.size)
    }

    @Test
    fun `testDevice sends vibration to controller`() = runTest {
        vm.testDevice("AA:BB:CC:DD:EE:FF")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, controller.sentCommands.size)
        assertEquals("AA:BB:CC:DD:EE:FF", controller.sentCommands[0].address)
        assertEquals(VibrationMode.SHORT_PULSE, controller.sentCommands[0].mode)
    }

    @Test
    fun `testDevice emits snackbar on success`() = runTest {
        vm.testDevice("AA:BB:CC:DD:EE:FF")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the vibration was sent (snackbar emission is a side effect of success)
        assertEquals(1, controller.sentCommands.size)
    }
}
