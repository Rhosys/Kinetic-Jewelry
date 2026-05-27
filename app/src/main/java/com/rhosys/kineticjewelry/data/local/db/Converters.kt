package com.rhosys.kineticjewelry.data.local.db

import com.rhosys.kineticjewelry.domain.model.ProtocolVersion
import com.rhosys.kineticjewelry.domain.model.VibrationMode

object Converters {
    fun vibrationModeToInt(mode: VibrationMode): Int = mode.stableId
    fun intToVibrationMode(id: Int): VibrationMode = VibrationMode.fromStableId(id)
    fun nullableVibrationModeToInt(mode: VibrationMode?): Int? = mode?.stableId
    fun intToNullableVibrationMode(id: Int?): VibrationMode? = id?.let { VibrationMode.fromStableId(it) }
    fun protocolVersionToInt(version: ProtocolVersion): Int = version.value
    fun intToProtocolVersion(value: Int): ProtocolVersion = ProtocolVersion.fromInt(value)
}
