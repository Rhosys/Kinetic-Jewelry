package ch.rhosys.lyra.data.local.db

import ch.rhosys.lyra.domain.model.ProtocolVersion
import ch.rhosys.lyra.domain.model.VibrationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomTypeConvertersTest {

    // ── VibrationMode ────────────────────────────────────────────────────────

    @Test
    fun `vibrationModeToInt stores stableId`() {
        VibrationMode.entries.forEach { mode ->
            assertEquals(mode.stableId, Converters.vibrationModeToInt(mode))
        }
    }

    @Test
    fun `intToVibrationMode restores correct mode`() {
        VibrationMode.entries.forEach { mode ->
            assertEquals(mode, Converters.intToVibrationMode(mode.stableId))
        }
    }

    @Test
    fun `intToVibrationMode unknown id falls back to SHORT_PULSE`() {
        assertEquals(VibrationMode.SHORT_PULSE, Converters.intToVibrationMode(999))
        assertEquals(VibrationMode.SHORT_PULSE, Converters.intToVibrationMode(0))
        assertEquals(VibrationMode.SHORT_PULSE, Converters.intToVibrationMode(-1))
    }

    @Test
    fun `nullableVibrationModeToInt stores null as null`() {
        assertNull(Converters.nullableVibrationModeToInt(null))
    }

    @Test
    fun `nullableVibrationModeToInt stores mode stableId`() {
        assertEquals(VibrationMode.SOS.stableId, Converters.nullableVibrationModeToInt(VibrationMode.SOS))
    }

    @Test
    fun `intToNullableVibrationMode restores mode`() {
        assertEquals(VibrationMode.SOS, Converters.intToNullableVibrationMode(VibrationMode.SOS.stableId))
    }

    @Test
    fun `intToNullableVibrationMode null input returns null`() {
        assertNull(Converters.intToNullableVibrationMode(null))
    }

    // ── ProtocolVersion ──────────────────────────────────────────────────────

    @Test
    fun `protocolVersionToInt stores value`() {
        ProtocolVersion.entries.forEach { v ->
            assertEquals(v.value, Converters.protocolVersionToInt(v))
        }
    }

    @Test
    fun `intToProtocolVersion restores correct version`() {
        ProtocolVersion.entries.forEach { v ->
            assertEquals(v, Converters.intToProtocolVersion(v.value))
        }
    }

    @Test
    fun `intToProtocolVersion unknown value falls back to V1`() {
        assertEquals(ProtocolVersion.V1, Converters.intToProtocolVersion(99))
    }
}
