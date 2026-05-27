package com.rhosys.kineticjewelry.data.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import kotlinx.coroutines.channels.Channel

private val FIRMWARE_CHAR_UUID = java.util.UUID.fromString("0000b2f0-0004-1000-8000-00805f9b34fb")

class BleGattCallback(
    private val events: Channel<GattEvent>,
) : BluetoothGattCallback() {

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            events.trySend(GattEvent.Connected(gatt))
        } else {
            events.trySend(GattEvent.Disconnected(gatt.device.address))
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            events.trySend(GattEvent.ServicesDiscovered(gatt))
        } else {
            events.trySend(GattEvent.Error("Service discovery failed: $status"))
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
    ) {
        events.trySend(GattEvent.CharacteristicWritten(status == BluetoothGatt.GATT_SUCCESS))
    }

    @Suppress("DEPRECATION")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
    ) {
        if (characteristic.uuid == FIRMWARE_CHAR_UUID && status == BluetoothGatt.GATT_SUCCESS) {
            val version = characteristic.value?.firstOrNull()?.toInt() ?: 1
            events.trySend(GattEvent.FirmwareVersionRead(version))
        }
    }
}
