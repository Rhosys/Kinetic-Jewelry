package com.rhosys.kineticjewelry.data.bluetooth

import android.bluetooth.BluetoothGatt

sealed class GattEvent {
    data class Connected(val gatt: BluetoothGatt) : GattEvent()
    data class ServicesDiscovered(val gatt: BluetoothGatt) : GattEvent()
    data class CharacteristicWritten(val success: Boolean) : GattEvent()
    data class FirmwareVersionRead(val version: Int) : GattEvent()
    data class Disconnected(val address: String) : GattEvent()
    data class Error(val message: String) : GattEvent()
}
