package com.rhosys.kineticjewelry.usecase

import com.rhosys.kineticjewelry.domain.model.AppFilter
import com.rhosys.kineticjewelry.domain.model.BluetoothDeviceInfo
import com.rhosys.kineticjewelry.domain.model.ConnectionState
import com.rhosys.kineticjewelry.domain.model.ContactFilter
import com.rhosys.kineticjewelry.domain.model.VibrationMode
import com.rhosys.kineticjewelry.domain.usecase.ProcessNotificationUseCase
import com.rhosys.kineticjewelry.fake.FakeAppFilterRepository
import com.rhosys.kineticjewelry.fake.FakeBluetoothController
import com.rhosys.kineticjewelry.fake.FakeBluetoothDeviceRepository
import com.rhosys.kineticjewelry.fake.FakeContactFilterRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProcessNotificationUseCaseTest {

    private lateinit var appRepo: FakeAppFilterRepository
    private lateinit var contactRepo: FakeContactFilterRepository
    private lateinit var deviceRepo: FakeBluetoothDeviceRepository
    private lateinit var btController: FakeBluetoothController
    private lateinit var useCase: ProcessNotificationUseCase

    private val alertDevice = BluetoothDeviceInfo(
        address = "AA:BB:CC:DD:EE:FF",
        name = "Ring",
        isAlertEnabled = true,
        connectionState = ConnectionState.DISCONNECTED,
    )

    private fun watchedApp(
        pkg: String = "com.example",
        mode: VibrationMode = VibrationMode.SHORT_PULSE,
        contactLevelEnabled: Boolean = false,
    ) = AppFilter(pkg, "Example", isWatched = true, vibrationMode = mode, isContactLevelEnabled = contactLevelEnabled)

    @Before
    fun setUp() {
        appRepo = FakeAppFilterRepository()
        contactRepo = FakeContactFilterRepository()
        deviceRepo = FakeBluetoothDeviceRepository()
        btController = FakeBluetoothController()
        useCase = ProcessNotificationUseCase(appRepo, contactRepo, deviceRepo, btController)
    }

    // ── Scenario 1: un-watched app ───────────────────────────────────────────

    @Test
    fun `unwatched app sends no vibration`() = runBlocking {
        appRepo.upsert(AppFilter("com.example", "Example", isWatched = false, VibrationMode.SHORT_PULSE, false))
        deviceRepo.upsert(alertDevice)

        useCase.execute("com.example", "", "Alice")

        assertTrue(btController.sentCommands.isEmpty())
    }

    // ── Scenario 2: blank contactName (noise filter) ─────────────────────────

    @Test
    fun `blank contactName sends no vibration and creates no contact entry`() = runBlocking {
        appRepo.upsert(watchedApp(contactLevelEnabled = true))
        deviceRepo.upsert(alertDevice)

        useCase.execute("com.example", "", null)
        useCase.execute("com.example", "", "")

        assertTrue(btController.sentCommands.isEmpty())
        assertNull(contactRepo.get("com.example", "", ""))
    }

    // ── Scenario 3: contact-level disabled ──────────────────────────────────

    @Test
    fun `contact-level disabled uses app mode and creates no contact entry`() = runBlocking {
        appRepo.upsert(watchedApp(mode = VibrationMode.LONG_PULSE, contactLevelEnabled = false))
        deviceRepo.upsert(alertDevice)

        useCase.execute("com.example", "", "Alice")

        assertEquals(1, btController.sentCommands.size)
        assertEquals(VibrationMode.LONG_PULSE, btController.sentCommands[0].mode)
        assertNull(contactRepo.get("com.example", "", "Alice"))
    }

    // ── Scenario 4: contact-level enabled, new contact ──────────────────────

    @Test
    fun `new contact is auto-created with null inherit and app mode fires`() = runBlocking {
        appRepo.upsert(watchedApp(mode = VibrationMode.HEARTBEAT, contactLevelEnabled = true))
        deviceRepo.upsert(alertDevice)

        useCase.execute("com.example", "", "Alice")

        val created = contactRepo.get("com.example", "", "Alice")
        assertNotNull("Contact should be auto-created", created)
        assertNull("New contact isWatched should be null (inherit)", created!!.isWatched)
        assertNull("New contact vibrationMode should be null (inherit)", created.vibrationMode)
        assertEquals(1, btController.sentCommands.size)
        assertEquals(VibrationMode.HEARTBEAT, btController.sentCommands[0].mode)
    }

    // ── Scenario 5: explicit mode override on contact ────────────────────────

    @Test
    fun `contact explicit vibration mode overrides app default`() = runBlocking {
        appRepo.upsert(watchedApp(mode = VibrationMode.SHORT_PULSE, contactLevelEnabled = true))
        contactRepo.upsert(ContactFilter("com.example", "", "Alice", isWatched = true, VibrationMode.SOS))
        deviceRepo.upsert(alertDevice)

        useCase.execute("com.example", "", "Alice")

        assertEquals(1, btController.sentCommands.size)
        assertEquals(VibrationMode.SOS, btController.sentCommands[0].mode)
    }

    // ── Scenario 6: contact isWatched=false ──────────────────────────────────

    @Test
    fun `contact with isWatched false blocks vibration even if app is watched`() = runBlocking {
        appRepo.upsert(watchedApp(contactLevelEnabled = true))
        contactRepo.upsert(ContactFilter("com.example", "", "Alice", isWatched = false, null))
        deviceRepo.upsert(alertDevice)

        useCase.execute("com.example", "", "Alice")

        assertTrue(btController.sentCommands.isEmpty())
    }

    // ── Scenario 7: group rule fires when no sender-specific rule ────────────

    @Test
    fun `group rule fires when sender has no explicit rule`() = runBlocking {
        appRepo.upsert(watchedApp(mode = VibrationMode.SHORT_PULSE, contactLevelEnabled = true))
        // Group entry (groupName=Work, contactName="" is the group placeholder)
        contactRepo.upsert(ContactFilter("com.example", "Work", "", isWatched = true, VibrationMode.DOUBLE_TAP))
        deviceRepo.upsert(alertDevice)

        useCase.execute("com.example", "Work", "Alice")

        assertEquals(1, btController.sentCommands.size)
        assertEquals(VibrationMode.DOUBLE_TAP, btController.sentCommands[0].mode)
    }

    // ── Scenario 8: sender-within-group overrides group rule ─────────────────

    @Test
    fun `sender-specific rule overrides group rule`() = runBlocking {
        appRepo.upsert(watchedApp(mode = VibrationMode.SHORT_PULSE, contactLevelEnabled = true))
        contactRepo.upsert(ContactFilter("com.example", "Work", "", isWatched = true, VibrationMode.DOUBLE_TAP))
        contactRepo.upsert(ContactFilter("com.example", "Work", "Alice", isWatched = true, VibrationMode.ESCALATING))
        deviceRepo.upsert(alertDevice)

        useCase.execute("com.example", "Work", "Alice")

        assertEquals(1, btController.sentCommands.size)
        assertEquals(VibrationMode.ESCALATING, btController.sentCommands[0].mode)
    }

    // ── Scenario 9: multiple alert-enabled devices ───────────────────────────

    @Test
    fun `sendVibration called once per alert-enabled device`() = runBlocking {
        appRepo.upsert(watchedApp())
        deviceRepo.upsert(alertDevice)
        deviceRepo.upsert(BluetoothDeviceInfo("11:22:33:44:55:66", "Bracelet", true, ConnectionState.DISCONNECTED))

        useCase.execute("com.example", "", "Alice")

        assertEquals(2, btController.sentCommands.size)
        val addresses = btController.sentCommands.map { it.address }.toSet()
        assertTrue(addresses.contains("AA:BB:CC:DD:EE:FF"))
        assertTrue(addresses.contains("11:22:33:44:55:66"))
    }

    // ── Scenario 10: no alert-enabled devices ────────────────────────────────

    @Test
    fun `no alert-enabled devices means sendVibration never called`() = runBlocking {
        appRepo.upsert(watchedApp())
        deviceRepo.upsert(BluetoothDeviceInfo("AA:BB:CC:DD:EE:FF", "Ring", isAlertEnabled = false, ConnectionState.DISCONNECTED))

        useCase.execute("com.example", "", "Alice")

        assertTrue(btController.sentCommands.isEmpty())
    }
}
