package ch.rhosys.lyra.usecase

import ch.rhosys.lyra.data.AppLogger
import ch.rhosys.lyra.domain.model.AppFilter
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.ConnectionState
import ch.rhosys.lyra.domain.model.ContactFilter
import ch.rhosys.lyra.domain.model.MultiDeviceMode
import ch.rhosys.lyra.domain.model.VibrationMode
import ch.rhosys.lyra.domain.usecase.DeviceVibrationDispatcher
import ch.rhosys.lyra.domain.usecase.ProcessNotificationUseCase
import ch.rhosys.lyra.fake.FakeAppFilterRepository
import ch.rhosys.lyra.fake.FakeAppSettingsProvider
import ch.rhosys.lyra.fake.FakeBluetoothController
import ch.rhosys.lyra.fake.FakeBluetoothDeviceRepository
import ch.rhosys.lyra.fake.FakeContactFilterRepository
import ch.rhosys.lyra.fake.FakePhoneVibrator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProcessNotificationUseCaseTest {
    private lateinit var appRepo: FakeAppFilterRepository
    private lateinit var contactRepo: FakeContactFilterRepository
    private lateinit var deviceRepo: FakeBluetoothDeviceRepository
    private lateinit var btController: FakeBluetoothController
    private lateinit var phoneVibrator: FakePhoneVibrator
    private lateinit var appSettings: FakeAppSettingsProvider
    private lateinit var useCase: ProcessNotificationUseCase

    private fun buildUseCase(): ProcessNotificationUseCase =
        ProcessNotificationUseCase(
            appRepo,
            contactRepo,
            DeviceVibrationDispatcher(deviceRepo, btController, phoneVibrator, appSettings, AppLogger()),
            AppLogger(),
        )

    // Favorited + enabled — the only combination that actually receives real vibrations.
    private val alertDevice =
        BluetoothDeviceInfo(
            address = "AA:BB:CC:DD:EE:FF",
            name = "Ring",
            isAlertEnabled = true,
            connectionState = ConnectionState.DISCONNECTED,
            isFavorite = true,
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
        phoneVibrator = FakePhoneVibrator()
        appSettings = FakeAppSettingsProvider()
        useCase = buildUseCase()
    }

    // ── Scenario 1: un-watched app ───────────────────────────────────────────

    @Test
    fun `unwatched app sends no vibration`() =
        runTest {
            appRepo.upsert(AppFilter("com.example", "Example", isWatched = false, VibrationMode.SHORT_PULSE, false))
            deviceRepo.upsert(alertDevice)

            useCase.execute("com.example", "", "Alice")

            assertTrue(btController.sentCommands.isEmpty())
        }

    // ── Scenario 2: blank contactName (noise filter) ─────────────────────────

    @Test
    fun `blank contactName sends no vibration and creates no contact entry`() =
        runTest {
            appRepo.upsert(watchedApp(contactLevelEnabled = true))
            deviceRepo.upsert(alertDevice)

            useCase.execute("com.example", "", null)
            useCase.execute("com.example", "", "")

            assertTrue(btController.sentCommands.isEmpty())
            assertNull(contactRepo.get("com.example", "", ""))
        }

    // ── Scenario 3: contact-level disabled ──────────────────────────────────

    @Test
    fun `contact-level disabled uses app mode and creates no contact entry`() =
        runTest {
            appRepo.upsert(watchedApp(mode = VibrationMode.LONG_PULSE, contactLevelEnabled = false))
            deviceRepo.upsert(alertDevice)

            useCase.execute("com.example", "", "Alice")

            assertEquals(1, btController.sentCommands.size)
            assertEquals(VibrationMode.LONG_PULSE.blocks, btController.sentCommands[0].blocks)
            assertNull(contactRepo.get("com.example", "", "Alice"))
        }

    // ── Scenario 4: contact-level enabled, unknown sender is whitelisted out ─────────────────────────

    @Test
    fun `unknown sender is not auto-created and does not vibrate`() =
        runTest {
            appRepo.upsert(watchedApp(mode = VibrationMode.HEARTBEAT, contactLevelEnabled = true))
            deviceRepo.upsert(alertDevice)

            useCase.execute("com.example", "", "Alice")

            assertNull("Unknown sender must not be auto-added", contactRepo.get("com.example", "", "Alice"))
            assertTrue("Contact-level filtering is a whitelist — unknown senders must not vibrate", btController.sentCommands.isEmpty())
            assertTrue(phoneVibrator.sentCommands.isEmpty())
        }

    // ── Scenario 4b: removing a watched user stops their vibrations ──────────────────────────────────

    @Test
    fun `removed user no longer vibrates`() =
        runTest {
            appRepo.upsert(watchedApp(mode = VibrationMode.HEARTBEAT, contactLevelEnabled = true))
            contactRepo.upsert(ContactFilter("com.example", "", "Alice", isWatched = true, null))
            deviceRepo.upsert(alertDevice)

            // Alice is watched → vibrates
            useCase.execute("com.example", "", "Alice")
            assertEquals(1, btController.sentCommands.size)

            // User removes Alice
            contactRepo.delete("com.example", "", "Alice")
            btController.sentCommands.clear()
            phoneVibrator.sentCommands.clear()

            useCase.execute("com.example", "", "Alice")
            assertTrue("Removed user must not vibrate", btController.sentCommands.isEmpty())
            assertTrue(phoneVibrator.sentCommands.isEmpty())
        }

    // ── Scenario 5: explicit mode override on contact ────────────────────────

    @Test
    fun `contact explicit vibration mode overrides app default`() =
        runTest {
            appRepo.upsert(watchedApp(mode = VibrationMode.SHORT_PULSE, contactLevelEnabled = true))
            contactRepo.upsert(ContactFilter("com.example", "", "Alice", isWatched = true, VibrationMode.SOS))
            deviceRepo.upsert(alertDevice)

            useCase.execute("com.example", "", "Alice")

            assertEquals(1, btController.sentCommands.size)
            assertEquals(VibrationMode.SOS.blocks, btController.sentCommands[0].blocks)
        }

    // ── Scenario 6: contact isWatched=false ──────────────────────────────────

    @Test
    fun `contact with isWatched false blocks vibration even if app is watched`() =
        runTest {
            appRepo.upsert(watchedApp(contactLevelEnabled = true))
            contactRepo.upsert(ContactFilter("com.example", "", "Alice", isWatched = false, null))
            deviceRepo.upsert(alertDevice)

            useCase.execute("com.example", "", "Alice")

            assertTrue(btController.sentCommands.isEmpty())
        }

    // ── Scenario 7: group rule fires when no sender-specific rule ────────────

    @Test
    fun `group rule fires when sender has no explicit rule`() =
        runTest {
            appRepo.upsert(watchedApp(mode = VibrationMode.SHORT_PULSE, contactLevelEnabled = true))
            contactRepo.upsert(ContactFilter("com.example", "Work", "", isWatched = true, VibrationMode.LONG_PULSE))
            deviceRepo.upsert(alertDevice)

            useCase.execute("com.example", "Work", "Alice")

            assertEquals(1, btController.sentCommands.size)
            assertEquals(VibrationMode.LONG_PULSE.blocks, btController.sentCommands[0].blocks)
        }

    // ── Scenario 8: sender-within-group overrides group rule ─────────────────

    @Test
    fun `sender-specific rule overrides group rule`() =
        runTest {
            appRepo.upsert(watchedApp(mode = VibrationMode.SHORT_PULSE, contactLevelEnabled = true))
            contactRepo.upsert(ContactFilter("com.example", "Work", "", isWatched = true, VibrationMode.LONG_PULSE))
            contactRepo.upsert(ContactFilter("com.example", "Work", "Alice", isWatched = true, VibrationMode.ESCALATING))
            deviceRepo.upsert(alertDevice)

            useCase.execute("com.example", "Work", "Alice")

            assertEquals(1, btController.sentCommands.size)
            assertEquals(VibrationMode.ESCALATING.blocks, btController.sentCommands[0].blocks)
        }

    // ── Scenario 9: multiple alert-enabled devices ───────────────────────────

    @Test
    fun `sendVibration called once per alert-enabled device`() =
        runTest {
            appRepo.upsert(watchedApp())
            deviceRepo.upsert(alertDevice)
            deviceRepo.upsert(
                BluetoothDeviceInfo("11:22:33:44:55:66", "Bracelet", true, ConnectionState.DISCONNECTED, isFavorite = true),
            )

            useCase.execute("com.example", "", "Alice")

            assertEquals(2, btController.sentCommands.size)
            val addresses = btController.sentCommands.map { it.address }.toSet()
            assertTrue(addresses.contains("AA:BB:CC:DD:EE:FF"))
            assertTrue(addresses.contains("11:22:33:44:55:66"))
        }

    // ── Scenario 10: no alert-enabled devices ────────────────────────────────

    @Test
    fun `no alert-enabled devices means sendVibration never called`() =
        runTest {
            appRepo.upsert(watchedApp())
            deviceRepo.upsert(
                BluetoothDeviceInfo(
                    "AA:BB:CC:DD:EE:FF", "Ring", isAlertEnabled = false, ConnectionState.DISCONNECTED,
                    isFavorite = true,
                ),
            )

            useCase.execute("com.example", "", "Alice")

            assertTrue(btController.sentCommands.isEmpty())
        }

    @Test
    fun `non-favorite device never receives vibrations even when isAlertEnabled is true`() =
        runTest {
            appRepo.upsert(watchedApp())
            deviceRepo.upsert(
                BluetoothDeviceInfo("AA:BB:CC:DD:EE:FF", "Ring", isAlertEnabled = true, ConnectionState.DISCONNECTED, isFavorite = false),
            )

            useCase.execute("com.example", "", "Alice")

            assertTrue(btController.sentCommands.isEmpty())
        }

    // ── Scenario 11: FIRST_WINS stops after first success ────────────────────

    @Test
    fun `FIRST_WINS mode sends to first device only`() =
        runTest {
            appSettings = FakeAppSettingsProvider(mode = MultiDeviceMode.FIRST_WINS)
            useCase = buildUseCase()

            appRepo.upsert(watchedApp())
            deviceRepo.upsert(alertDevice)
            deviceRepo.upsert(
                BluetoothDeviceInfo("11:22:33:44:55:66", "Bracelet", true, ConnectionState.DISCONNECTED, isFavorite = true),
            )

            useCase.execute("com.example", "", "Alice")

            assertEquals(1, btController.sentCommands.size)
        }

    // ── Scenario 12: disabled device is skipped ──────────────────────────────

    @Test
    fun `disabled device is skipped`() =
        runTest {
            appRepo.upsert(watchedApp())
            val disabledDevice =
                alertDevice.copy(
                    disabledUntil = System.currentTimeMillis() + 60 * 60 * 1_000L,
                )
            deviceRepo.upsert(disabledDevice)

            useCase.execute("com.example", "", "Alice")

            assertTrue(btController.sentCommands.isEmpty())
        }

    // ── Scenario 13: call category ────────────────────────────────────────────

    @Test
    fun `first call-category notification marks app as having calls`() =
        runTest {
            appRepo.upsert(watchedApp())
            deviceRepo.upsert(alertDevice)

            useCase.execute("com.example", "", "Alice", category = android.app.Notification.CATEGORY_CALL)

            assertTrue(appRepo.getByPackageName("com.example")!!.hasCallCategory)
        }

    @Test
    fun `call with no call-specific mode falls back to app default vibration`() =
        runTest {
            appRepo.upsert(watchedApp(mode = VibrationMode.LONG_PULSE))
            deviceRepo.upsert(alertDevice)

            useCase.execute("com.example", "", "Alice", category = android.app.Notification.CATEGORY_CALL)

            assertEquals(1, btController.sentCommands.size)
            assertEquals(VibrationMode.LONG_PULSE.blocks, btController.sentCommands[0].blocks)
        }

    @Test
    fun `call uses call-specific vibration mode when configured`() =
        runTest {
            appRepo.upsert(
                watchedApp(mode = VibrationMode.SHORT_PULSE).copy(
                    hasCallCategory = true,
                    callVibrationMode = VibrationMode.SOS,
                ),
            )
            deviceRepo.upsert(alertDevice)

            useCase.execute("com.example", "", "Alice", category = android.app.Notification.CATEGORY_CALL)

            assertEquals(1, btController.sentCommands.size)
            assertEquals(VibrationMode.SOS.blocks, btController.sentCommands[0].blocks)
        }

    @Test
    fun `call bypasses contact-level whitelist`() =
        runTest {
            // Contact-level is a whitelist and Alice has no rule, so a normal message would be
            // dropped (Scenario 4) — but a call notification should still ring regardless.
            appRepo.upsert(watchedApp(mode = VibrationMode.HEARTBEAT, contactLevelEnabled = true))
            deviceRepo.upsert(alertDevice)

            useCase.execute("com.example", "", "Alice", category = android.app.Notification.CATEGORY_CALL)

            assertEquals(1, btController.sentCommands.size)
            assertEquals(VibrationMode.HEARTBEAT.blocks, btController.sentCommands[0].blocks)
        }

    @Test
    fun `regular message does not mark app as having calls`() =
        runTest {
            appRepo.upsert(watchedApp())
            deviceRepo.upsert(alertDevice)

            useCase.execute("com.example", "", "Alice")

            assertTrue(!appRepo.getByPackageName("com.example")!!.hasCallCategory)
        }
}
