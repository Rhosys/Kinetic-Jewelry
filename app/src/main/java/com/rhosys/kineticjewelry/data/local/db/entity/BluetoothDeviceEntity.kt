package com.rhosys.kineticjewelry.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rhosys.kineticjewelry.domain.model.BluetoothDeviceInfo
import com.rhosys.kineticjewelry.domain.model.ConnectionState

@Entity(tableName = "bluetooth_devices")
data class BluetoothDeviceEntity(
    @PrimaryKey val address: String,
    val name: String,
    val isAlertEnabled: Boolean,
    @ColumnInfo(name = "firmware_protocol_version", defaultValue = "1")
    val firmwareProtocolVersion: Int = 1,
) {
    fun toDomain(): BluetoothDeviceInfo = BluetoothDeviceInfo(
        address = address,
        name = name,
        isAlertEnabled = isAlertEnabled,
        connectionState = ConnectionState.DISCONNECTED,
    )

    companion object {
        fun fromDomain(device: BluetoothDeviceInfo, firmwareProtocolVersion: Int = 1) =
            BluetoothDeviceEntity(
                address = device.address,
                name = device.name,
                isAlertEnabled = device.isAlertEnabled,
                firmwareProtocolVersion = firmwareProtocolVersion,
            )
    }
}
