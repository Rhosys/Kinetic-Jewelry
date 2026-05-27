package com.rhosys.kineticjewelry.domain.model

data class BluetoothDeviceInfo(
    val address: String,
    val name: String,
    val isAlertEnabled: Boolean,
    val connectionState: ConnectionState,
)
