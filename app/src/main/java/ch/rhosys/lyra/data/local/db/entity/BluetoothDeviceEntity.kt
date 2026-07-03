package ch.rhosys.lyra.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.ConnectionState
import ch.rhosys.lyra.domain.model.DeviceType

@Entity(tableName = "bluetooth_devices")
data class BluetoothDeviceEntity(
    @PrimaryKey val address: String,
    val name: String,
    val isAlertEnabled: Boolean,
    @ColumnInfo(name = "firmware_protocol_version", defaultValue = "1")
    val firmwareProtocolVersion: Int = 1,
    @ColumnInfo(name = "device_type", defaultValue = "BLE_JEWELRY")
    val deviceType: String = DeviceType.BLE_JEWELRY.name,
    @ColumnInfo(name = "connection_timeout_ms")
    val connectionTimeoutMs: Long? = null,
    @ColumnInfo(name = "disabled_until")
    val disabledUntil: Long? = null,
    @ColumnInfo(name = "consecutive_timeouts", defaultValue = "0")
    val consecutiveTimeouts: Int = 0,
    @ColumnInfo(name = "last_success_at")
    val lastSuccessAt: Long? = null,
    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    val isFavorite: Boolean = false,
) {
    fun toDomain(): BluetoothDeviceInfo =
        BluetoothDeviceInfo(
            address = address,
            name = name,
            isAlertEnabled = isAlertEnabled,
            connectionState = ConnectionState.DISCONNECTED,
            deviceType = runCatching { DeviceType.valueOf(deviceType) }.getOrDefault(DeviceType.BLE_JEWELRY),
            connectionTimeoutMs = connectionTimeoutMs,
            disabledUntil = disabledUntil,
            consecutiveTimeouts = consecutiveTimeouts,
            lastSuccessAt = lastSuccessAt,
            isFavorite = isFavorite,
        )

    companion object {
        fun fromDomain(
            device: BluetoothDeviceInfo,
            firmwareProtocolVersion: Int = 1,
        ) = BluetoothDeviceEntity(
            address = device.address,
            name = device.name,
            isAlertEnabled = device.isAlertEnabled,
            firmwareProtocolVersion = firmwareProtocolVersion,
            deviceType = device.deviceType.name,
            connectionTimeoutMs = device.connectionTimeoutMs,
            disabledUntil = device.disabledUntil,
            consecutiveTimeouts = device.consecutiveTimeouts,
            lastSuccessAt = device.lastSuccessAt,
            isFavorite = device.isFavorite,
        )
    }
}
