package ch.rhosys.lyra.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelInvariantTest {

    // ── ProtocolVersion ──────────────────────────────────────────────────────

    @Test
    fun `fromInt returns V1 for value 1`() {
        assertEquals(ProtocolVersion.V1, ProtocolVersion.fromInt(1))
    }

    @Test
    fun `fromInt falls back to V1 for unknown value`() {
        assertEquals(ProtocolVersion.V1, ProtocolVersion.fromInt(99))
        assertEquals(ProtocolVersion.V1, ProtocolVersion.fromInt(0))
        assertEquals(ProtocolVersion.V1, ProtocolVersion.fromInt(-1))
    }

    @Test
    fun `ProtocolVersion values are positive`() {
        ProtocolVersion.entries.forEach { v ->
            assertTrue("${v.name} value must be > 0", v.value > 0)
        }
    }

    // ── VibrationBlock ───────────────────────────────────────────────────────

    @Test
    fun `all block IDs are unique`() {
        val ids = VibrationBlock.entries.map { it.id }
        assertEquals("Duplicate block IDs found", ids.distinct().size, ids.size)
    }

    @Test
    fun `all block IDs are non-zero`() {
        VibrationBlock.entries.forEach { block ->
            assertNotEquals("Block ${block.name} uses reserved ID 0x00", 0.toByte(), block.id)
        }
    }

    @Test
    fun `all block durations are positive`() {
        VibrationBlock.entries.forEach { block ->
            assertTrue("${block.name}.durationMs must be > 0", block.durationMs > 0)
        }
    }

    @Test
    fun `all blocks were introduced in a known ProtocolVersion`() {
        VibrationBlock.entries.forEach { block ->
            assertTrue(
                "${block.name}.since must be a valid ProtocolVersion",
                ProtocolVersion.entries.contains(block.since),
            )
        }
    }

    @Test
    fun `motorOn is true only for blocks with id below 0xA`() {
        VibrationBlock.entries.forEach { block ->
            assertEquals(
                "${block.name}.motorOn mismatch",
                block.id.toInt() < 0xA,
                block.motorOn,
            )
        }
    }

    @Test
    fun `pause blocks have shorter duration than buzz blocks at same tier`() {
        assertTrue(VibrationBlock.SHORT_PAUSE.durationMs < VibrationBlock.SHORT_BUZZ.durationMs)
        assertTrue(VibrationBlock.MEDIUM_PAUSE.durationMs < VibrationBlock.MEDIUM_BUZZ.durationMs)
    }

    // ── VibrationMode ────────────────────────────────────────────────────────

    @Test
    fun `fromStableId returns correct mode`() {
        VibrationMode.entries.forEach { mode ->
            assertEquals(mode, VibrationMode.fromStableId(mode.stableId))
        }
    }

    @Test
    fun `fromStableId falls back to SHORT_PULSE for unknown id`() {
        assertEquals(VibrationMode.SHORT_PULSE, VibrationMode.fromStableId(999))
        assertEquals(VibrationMode.SHORT_PULSE, VibrationMode.fromStableId(0))
        assertEquals(VibrationMode.SHORT_PULSE, VibrationMode.fromStableId(-1))
    }

    @Test
    fun `all stableIds are unique`() {
        val ids = VibrationMode.entries.map { it.stableId }
        assertEquals("Duplicate stableIds found", ids.distinct().size, ids.size)
    }

    @Test
    fun `all stableIds are positive`() {
        VibrationMode.entries.forEach { mode ->
            assertTrue("${mode.name}.stableId must be > 0", mode.stableId > 0)
        }
    }

    @Test
    fun `every mode's blocks strictly alternate motor on and off`() {
        VibrationMode.entries.forEach { mode ->
            mode.blocks.zipWithNext().forEach { (a, b) ->
                assertNotEquals(
                    "${mode.name} has two consecutive ${if (a.motorOn) "on" else "off"} blocks " +
                        "(${a.name} followed by ${b.name}) — blocks must alternate buzz/pause",
                    a.motorOn,
                    b.motorOn,
                )
            }
        }
    }

    @Test
    fun `no mode has an empty block list`() {
        VibrationMode.entries.forEach { mode ->
            assertTrue("${mode.name} must have at least one block", mode.blocks.isNotEmpty())
        }
    }

    @Test
    fun `every mode has at least 3 blocks`() {
        VibrationMode.entries.forEach { mode ->
            assertTrue(
                "${mode.name} must have at least 3 blocks — single-block or two-block modes are not allowed",
                mode.blocks.size >= 3,
            )
        }
    }

    @Test
    fun `withoutTrailingPauses strips trailing pause blocks`() {
        val blocks = listOf(
            VibrationBlock.SHORT_BUZZ, VibrationBlock.SHORT_PAUSE,
            VibrationBlock.SHORT_BUZZ, VibrationBlock.SHORT_PAUSE,
        )
        assertEquals(
            listOf(VibrationBlock.SHORT_BUZZ, VibrationBlock.SHORT_PAUSE, VibrationBlock.SHORT_BUZZ),
            blocks.withoutTrailingPauses(),
        )
    }

    @Test
    fun `withoutTrailingPauses is a no-op when already ending in a buzz`() {
        val blocks = listOf(VibrationBlock.CLICK, VibrationBlock.SHORT_PAUSE, VibrationBlock.CLICK)
        assertEquals(blocks, blocks.withoutTrailingPauses())
    }

    @Test
    fun `withoutTrailingPauses can return empty when every block is a pause`() {
        val blocks = listOf(VibrationBlock.SHORT_PAUSE, VibrationBlock.MEDIUM_PAUSE)
        assertTrue(blocks.withoutTrailingPauses().isEmpty())
    }

    @Test
    fun `every mode's waveform never ends in a pause once trailing pauses are stripped`() {
        VibrationMode.entries.forEach { mode ->
            val effective = mode.blocks.withoutTrailingPauses()
            assertTrue("${mode.name} must not strip down to an empty waveform", effective.isNotEmpty())
            assertTrue("${mode.name}'s waveform must not end in a pause", effective.last().motorOn)
        }
    }

    @Test
    fun `totalDurationMs equals sum of block durations`() {
        VibrationMode.entries.forEach { mode ->
            val expected = mode.blocks.sumOf { it.durationMs }
            assertEquals("${mode.name}.totalDurationMs mismatch", expected, mode.totalDurationMs)
        }
    }

    @Test
    fun `SHORT_PULSE is three short buzzes separated by short pauses`() {
        assertEquals(
            listOf(
                VibrationBlock.SHORT_BUZZ, VibrationBlock.SHORT_PAUSE,
                VibrationBlock.SHORT_BUZZ, VibrationBlock.SHORT_PAUSE,
                VibrationBlock.SHORT_BUZZ, VibrationBlock.SHORT_PAUSE,
            ),
            VibrationMode.SHORT_PULSE.blocks,
        )
    }

    @Test
    fun `LONG_PULSE is two long buzzes separated by medium pauses`() {
        assertEquals(
            listOf(
                VibrationBlock.LONG_BUZZ, VibrationBlock.MEDIUM_PAUSE,
                VibrationBlock.LONG_BUZZ, VibrationBlock.MEDIUM_PAUSE,
            ),
            VibrationMode.LONG_PULSE.blocks,
        )
    }

    @Test
    fun `SOS has exactly 18 blocks`() {
        assertEquals(18, VibrationMode.SOS.blocks.size)
    }

    @Test
    fun `every mode fits in a single 32-block packet`() {
        VibrationMode.entries.forEach { mode ->
            assertTrue(
                "${mode.name} must fit in a single 32-block packet",
                mode.blocks.size <= 32,
            )
        }
    }

    @Test
    fun `default mode is SHORT_PULSE`() {
        assertEquals(VibrationMode.SHORT_PULSE, VibrationMode.default)
    }

    @Test
    fun `ESCALATING blocks are in non-decreasing buzz intensity order`() {
        val buzzBlocks = VibrationMode.ESCALATING.blocks.filter { it.durationMs >= 40 && it != VibrationBlock.SHORT_PAUSE && it != VibrationBlock.MEDIUM_PAUSE && it != VibrationBlock.LONG_PAUSE }
        val durations = buzzBlocks.map { it.durationMs }
        assertEquals(durations.sorted(), durations)
    }

    // ── ContactFilter ────────────────────────────────────────────────────────

    @Test
    fun `ContactFilter isWatched null means inherit`() {
        val contact = ContactFilter(
            packageName = "com.example.app",
            groupName = "",
            contactName = "Alice",
            isWatched = null,
            vibrationMode = null,
        )
        assertNull("null isWatched signals inherit from parent", contact.isWatched)
    }

    @Test
    fun `ContactFilter vibrationMode null means inherit`() {
        val contact = ContactFilter(
            packageName = "com.example.app",
            groupName = "",
            contactName = "Alice",
            isWatched = true,
            vibrationMode = null,
        )
        assertNull("null vibrationMode signals inherit from parent", contact.vibrationMode)
    }

    @Test
    fun `ContactFilter explicit isWatched false overrides even when parent is watched`() {
        val contact = ContactFilter(
            packageName = "com.example.app",
            groupName = "",
            contactName = "Alice",
            isWatched = false,
            vibrationMode = null,
        )
        assertNotNull(contact.isWatched)
        assertFalse(contact.isWatched!!)
    }

    @Test
    fun `ContactFilter groupName empty string denotes direct contact`() {
        val direct = ContactFilter("pkg", "", "Alice", null, null)
        val grouped = ContactFilter("pkg", "Work Group", "Alice", null, null)
        assertEquals("", direct.groupName)
        assertEquals("Work Group", grouped.groupName)
        assertNotEquals(direct, grouped)
    }

    @Test
    fun `ContactFilter composite key distinguishes same name in different apps`() {
        val inApp1 = ContactFilter("com.app.one", "", "Alice", true, null)
        val inApp2 = ContactFilter("com.app.two", "", "Alice", true, null)
        assertNotEquals(inApp1, inApp2)
    }

    @Test
    fun `ContactFilter explicit vibrationMode is preserved`() {
        val contact = ContactFilter(
            packageName = "com.example.app",
            groupName = "",
            contactName = "Alice",
            isWatched = true,
            vibrationMode = VibrationMode.SOS,
        )
        assertEquals(VibrationMode.SOS, contact.vibrationMode)
    }

    // ── ConnectionState ──────────────────────────────────────────────────────

    @Test
    fun `ConnectionState has exactly four values`() {
        assertEquals(4, ConnectionState.entries.size)
    }

    @Test
    fun `ConnectionState contains DISCONNECTED CONNECTING CONNECTED ERROR`() {
        val names = ConnectionState.entries.map { it.name }.toSet()
        assertTrue(names.contains("DISCONNECTED"))
        assertTrue(names.contains("CONNECTING"))
        assertTrue(names.contains("CONNECTED"))
        assertTrue(names.contains("ERROR"))
    }

    // ── BluetoothDeviceInfo ──────────────────────────────────────────────────

    @Test
    fun `BluetoothDeviceInfo address is the stable identity`() {
        val d1 = BluetoothDeviceInfo("AA:BB:CC:DD:EE:FF", "Ring", true, ConnectionState.CONNECTED)
        val d2 = BluetoothDeviceInfo("AA:BB:CC:DD:EE:FF", "Ring", true, ConnectionState.DISCONNECTED)
        assertNotEquals("Different connectionState means different value object", d1, d2)
        assertEquals(d1.address, d2.address)
    }

    @Test
    fun `BluetoothDeviceInfo isAlertEnabled false creates paired-only device`() {
        val device = BluetoothDeviceInfo("AA:BB:CC:DD:EE:FF", "Ring", false, ConnectionState.DISCONNECTED)
        assertFalse(device.isAlertEnabled)
    }

    @Test
    fun `BluetoothDeviceInfo copy can update connectionState`() {
        val original = BluetoothDeviceInfo("AA:BB:CC:DD:EE:FF", "Ring", true, ConnectionState.DISCONNECTED)
        val updated = original.copy(connectionState = ConnectionState.CONNECTED)
        assertEquals(ConnectionState.CONNECTED, updated.connectionState)
        assertEquals(original.address, updated.address)
        assertEquals(original.isAlertEnabled, updated.isAlertEnabled)
    }

    // ── NotificationEvent ────────────────────────────────────────────────────

    @Test
    fun `NotificationEvent postedAt defaults to positive value`() {
        val event = NotificationEvent(packageName = "com.example.app", senderName = "Alice", text = "Hi", category = null)
        assertTrue("postedAt must be a positive epoch millis", event.postedAt > 0)
    }

    @Test
    fun `NotificationEvent nullable fields can be null`() {
        val event = NotificationEvent(packageName = "com.example.app", senderName = null, text = null, category = null)
        assertNull(event.senderName)
        assertNull(event.text)
        assertNull(event.category)
    }

    @Test
    fun `NotificationEvent two events at different times are not equal`() {
        val t1 = NotificationEvent("pkg", "Alice", "Hi", null, postedAt = 1000L)
        val t2 = NotificationEvent("pkg", "Alice", "Hi", null, postedAt = 2000L)
        assertNotEquals(t1, t2)
    }

    @Test
    fun `NotificationEvent packageName is required`() {
        val event = NotificationEvent(packageName = "com.whatsapp", senderName = "Bob", text = "Hey", category = "msg")
        assertEquals("com.whatsapp", event.packageName)
    }
}
