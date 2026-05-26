package com.rhosys.kineticjewelry.data.local.db

import com.rhosys.kineticjewelry.domain.model.ProtocolVersion
import com.rhosys.kineticjewelry.domain.model.VibrationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomTypeConvertersTest {

    private val converters = RoomTypeConverters()

    // ── VibrationMode ────────────────────────────────────────────────────────

    @Test
    fun `vibrationModeToInt stores stableId`() {
        VibrationMode.entries.forEach { mode ->
            assertEquals(mode.stableId, converters.vibrationModeToInt(mode))
        }
    }

    @Test
    fun `intToVibrationMode restores correct mode`() {
        VibrationMode.entries.forEach { mode ->
            assertEquals(mode, converters.intToVibrationMode(mode.stableId))
        }
    }

    @Test
    fun `intToVibrationMode unknown id falls back to SHORT_PULSE`() {
        assertEquals(VibrationMode.SHORT_PULSE, converters.intToVibrationMode(999))
        assertEquals(VibrationMode.SHORT_PULSE, converters.intToVibrationMode(0))
        assertEquals(VibrationMode.SHORT_PULSE, converters.intToVibrationMode(-1))
    }

    @Test
    fun `nullableVibrationModeToInt stores null as null`() {
        assertNull(converters.nullableVibrationModeToInt(null))
    }

    @Test
    fun `nullableVibrationModeToInt stores mode stableId`() {
        assertEquals(VibrationMode.SOS.stableId, converters.nullableVibrationModeToInt(VibrationMode.SOS))
    }

    @Test
    fun `intToNullableVibrationMode restores mode`() {
        assertEquals(VibrationMode.SOS, converters.intToNullableVibrationMode(VibrationMode.SOS.stableId))
    }

    @Test
    fun `intToNullableVibrationMode null input returns null`() {
        assertNull(converters.intToNullableVibrationMode(null))
    }

    // ── ProtocolVersion ──────────────────────────────────────────────────────

    @Test
    fun `protocolVersionToInt stores value`() {
        ProtocolVersion.entries.forEach { v ->
            assertEquals(v.value, converters.protocolVersionToInt(v))
        }
    }

    @Test
    fun `intToProtocolVersion restores correct version`() {
        ProtocolVersion.entries.forEach { v ->
            assertEquals(v, converters.intToProtocolVersion(v.value))
        }
    }

    @Test
    fun `intToProtocolVersion unknown value falls back to V1`() {
        assertEquals(ProtocolVersion.V1, converters.intToProtocolVersion(99))
    }
}
