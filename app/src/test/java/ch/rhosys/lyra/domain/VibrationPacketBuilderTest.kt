package ch.rhosys.lyra.domain

import ch.rhosys.lyra.domain.model.ProtocolVersion
import ch.rhosys.lyra.domain.model.VibrationBlock
import ch.rhosys.lyra.domain.model.VibrationMode
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VibrationPacketBuilderTest {

    private val builder = VibrationPacketBuilder()

    // ── Packet structure ─────────────────────────────────────────────────────

    @Test
    fun `single-block mode produces one packet`() {
        val packets = builder.buildPackets(VibrationMode.SHORT_PULSE, ProtocolVersion.V1)
        assertEquals(1, packets.size)
    }

    @Test
    fun `packet header contains protocolVersion command and repeatCount`() {
        val packets = builder.buildPackets(VibrationMode.SHORT_PULSE, ProtocolVersion.V1, repeat = 3)
        val bytes = packets[0].first
        assertEquals(ProtocolVersion.V1.value.toByte(), bytes[0])   // protocolVersion
        assertEquals(0x01.toByte(), bytes[1])                        // VIBRATE command
        assertEquals(3.toByte(), bytes[2])                           // repeatCount
    }

    @Test
    fun `packet body contains block IDs after header`() {
        val packets = builder.buildPackets(VibrationMode.SHORT_PULSE, ProtocolVersion.V1)
        val bytes = packets[0].first
        assertEquals(4, bytes.size)  // 3-byte header + 1 block
        assertEquals(VibrationBlock.SHORT_BUZZ.id, bytes[3])
    }

    @Test
    fun `delay equals sum of block durations in packet`() {
        val packets = builder.buildPackets(VibrationMode.HEARTBEAT, ProtocolVersion.V1)
        assertEquals(1, packets.size)
        val expectedDelay = VibrationMode.HEARTBEAT.totalDurationMs
        assertEquals(expectedDelay, packets[0].second)
    }

    @Test
    fun `default repeat is 1`() {
        val packets = builder.buildPackets(VibrationMode.SHORT_PULSE, ProtocolVersion.V1)
        assertEquals(1.toByte(), packets[0].first[2])
    }

    // ── Multi-packet (SOS) ───────────────────────────────────────────────────

    @Test
    fun `SOS produces two packets`() {
        val packets = builder.buildPackets(VibrationMode.SOS, ProtocolVersion.V1)
        assertEquals(2, packets.size)
    }

    @Test
    fun `SOS first packet contains first 16 block IDs`() {
        val packets = builder.buildPackets(VibrationMode.SOS, ProtocolVersion.V1)
        val expectedIds = VibrationMode.SOS.blocks.take(16).map { it.id }.toByteArray()
        val actualIds = packets[0].first.drop(3).toByteArray()
        assertArrayEquals(expectedIds, actualIds)
    }

    @Test
    fun `SOS second packet contains remaining 2 block IDs`() {
        val packets = builder.buildPackets(VibrationMode.SOS, ProtocolVersion.V1)
        val expectedIds = VibrationMode.SOS.blocks.drop(16).map { it.id }.toByteArray()
        val actualIds = packets[1].first.drop(3).toByteArray()
        assertArrayEquals(expectedIds, actualIds)
    }

    @Test
    fun `SOS first packet delay matches its block durations`() {
        val packets = builder.buildPackets(VibrationMode.SOS, ProtocolVersion.V1)
        val expectedDelay = VibrationMode.SOS.blocks.take(16).sumOf { it.durationMs }
        assertEquals(expectedDelay, packets[0].second)
    }

    @Test
    fun `no packet exceeds 19 bytes (fits default ATT MTU)`() {
        VibrationMode.entries.forEach { mode ->
            builder.buildPackets(mode, ProtocolVersion.V1).forEachIndexed { i, (bytes, _) ->
                assertTrue(
                    "${mode.name} packet[$i] is ${bytes.size} bytes — exceeds 19",
                    bytes.size <= 19,
                )
            }
        }
    }

    // ── Protocol-version filtering ───────────────────────────────────────────

    @Test
    fun `all V1 blocks pass through on V1 firmware`() {
        // Every V1 block should survive when firmware is V1
        val packets = builder.buildPackets(VibrationMode.ESCALATING, ProtocolVersion.V1)
        val allIds = packets.flatMap { (bytes, _) -> bytes.drop(3).map { it } }
        val expectedIds = VibrationMode.ESCALATING.blocks.map { it.id }
        assertEquals(expectedIds, allIds)
    }

    // ── Fallback behaviour ───────────────────────────────────────────────────

    @Test
    fun `mode with all blocks filtered falls back to SHORT_PULSE`() {
        // Simulate by using a mode whose every block has since > firmware
        // We can't do this with current enums (all V1), so we test the fallback
        // indirectly: buildPackets must never return an empty list
        VibrationMode.entries.forEach { mode ->
            val packets = builder.buildPackets(mode, ProtocolVersion.V1)
            assertTrue("${mode.name} must produce at least one packet", packets.isNotEmpty())
            packets.forEach { (bytes, _) ->
                assertTrue("${mode.name} packet must have at least one block ID", bytes.size > 3)
            }
        }
    }

    // ── Packet size math ─────────────────────────────────────────────────────

    @Test
    fun `single-block packet is exactly 4 bytes`() {
        val packets = builder.buildPackets(VibrationMode.SHORT_PULSE, ProtocolVersion.V1)
        assertEquals(4, packets[0].first.size)
    }

    @Test
    fun `ESCALATING single packet is 3 header + 9 block bytes`() {
        val packets = builder.buildPackets(VibrationMode.ESCALATING, ProtocolVersion.V1)
        assertEquals(1, packets.size)
        assertEquals(12, packets[0].first.size)  // 3 + 9
    }
}
