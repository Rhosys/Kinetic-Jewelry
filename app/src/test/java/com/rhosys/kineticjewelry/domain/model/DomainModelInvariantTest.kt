package com.rhosys.kineticjewelry.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
    fun `no mode has an empty block list`() {
        VibrationMode.entries.forEach { mode ->
            assertTrue("${mode.name} must have at least one block", mode.blocks.isNotEmpty())
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
    fun `SHORT_PULSE totalDurationMs equals SHORT_BUZZ duration`() {
        assertEquals(VibrationBlock.SHORT_BUZZ.durationMs, VibrationMode.SHORT_PULSE.totalDurationMs)
    }

    @Test
    fun `SOS has exactly 18 blocks`() {
        assertEquals(18, VibrationMode.SOS.blocks.size)
    }

    @Test
    fun `SOS blocks split into exactly two packets of at most 16 blocks`() {
        val chunks = VibrationMode.SOS.blocks.chunked(16)
        assertEquals(2, chunks.size)
        assertTrue(chunks[0].size <= 16)
        assertTrue(chunks[1].size <= 16)
    }

    @Test
    fun `all other modes fit in a single packet`() {
        VibrationMode.entries.filter { it != VibrationMode.SOS }.forEach { mode ->
            assertTrue(
                "${mode.name} must fit in a single 16-block packet",
                mode.blocks.size <= 16,
            )
        }
    }

    @Test
    fun `default mode is SHORT_PULSE`() {
        assertEquals(VibrationMode.SHORT_PULSE, VibrationMode.default)
    }

    @Test
    fun `DOUBLE_TAP has symmetrical structure`() {
        val blocks = VibrationMode.DOUBLE_TAP.blocks
        assertEquals(3, blocks.size)
        assertEquals(blocks.first(), blocks.last())
    }

    @Test
    fun `ESCALATING blocks are in non-decreasing buzz intensity order`() {
        val buzzBlocks = VibrationMode.ESCALATING.blocks.filter { it.durationMs >= 40 && it != VibrationBlock.SHORT_PAUSE && it != VibrationBlock.MEDIUM_PAUSE && it != VibrationBlock.LONG_PAUSE }
        val durations = buzzBlocks.map { it.durationMs }
        assertEquals(durations.sorted(), durations)
    }
}
