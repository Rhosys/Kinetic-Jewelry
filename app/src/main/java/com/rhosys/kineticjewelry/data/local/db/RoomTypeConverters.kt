package com.rhosys.kineticjewelry.data.local.db

import androidx.room.TypeConverter
import com.rhosys.kineticjewelry.domain.model.ProtocolVersion
import com.rhosys.kineticjewelry.domain.model.VibrationMode

class RoomTypeConverters {
    @TypeConverter fun vibrationModeToInt(mode: VibrationMode): Int = Converters.vibrationModeToInt(mode)
    @TypeConverter fun intToVibrationMode(id: Int): VibrationMode = Converters.intToVibrationMode(id)
    @TypeConverter fun nullableVibrationModeToInt(mode: VibrationMode?): Int? = Converters.nullableVibrationModeToInt(mode)
    @TypeConverter fun intToNullableVibrationMode(id: Int?): VibrationMode? = Converters.intToNullableVibrationMode(id)
    @TypeConverter fun protocolVersionToInt(version: ProtocolVersion): Int = Converters.protocolVersionToInt(version)
    @TypeConverter fun intToProtocolVersion(value: Int): ProtocolVersion = Converters.intToProtocolVersion(value)
}
