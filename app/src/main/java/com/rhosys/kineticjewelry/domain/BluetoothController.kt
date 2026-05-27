package com.rhosys.kineticjewelry.domain

import com.rhosys.kineticjewelry.domain.model.BluetoothDeviceInfo
import com.rhosys.kineticjewelry.domain.model.VibrationMode
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>>
    val connectedDevices: StateFlow<List<BluetoothDeviceInfo>>

    suspend fun sendVibration(address: String, mode: VibrationMode): Result<Unit>
    fun releaseResources()
}
