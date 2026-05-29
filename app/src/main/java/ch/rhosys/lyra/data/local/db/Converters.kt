package ch.rhosys.lyra.data.local.db

import ch.rhosys.lyra.domain.model.ProtocolVersion
import ch.rhosys.lyra.domain.model.VibrationMode

object Converters {
    fun vibrationModeToInt(mode: VibrationMode): Int = mode.stableId
    fun intToVibrationMode(id: Int): VibrationMode = VibrationMode.fromStableId(id)
    fun nullableVibrationModeToInt(mode: VibrationMode?): Int? = mode?.stableId
    fun intToNullableVibrationMode(id: Int?): VibrationMode? = id?.let { VibrationMode.fromStableId(it) }
    fun protocolVersionToInt(version: ProtocolVersion): Int = version.value
    fun intToProtocolVersion(value: Int): ProtocolVersion = ProtocolVersion.fromInt(value)
}
