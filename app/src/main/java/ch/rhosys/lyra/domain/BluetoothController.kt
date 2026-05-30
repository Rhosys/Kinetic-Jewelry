package ch.rhosys.lyra.domain

import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.VibrationMode
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>>
    val connectedDevices: StateFlow<List<BluetoothDeviceInfo>>
    val scanResults: StateFlow<List<BluetoothDeviceInfo>>
    val isScanning: StateFlow<Boolean>

    suspend fun sendVibration(address: String, mode: VibrationMode): Result<Unit>
    fun startScan()
    fun stopScan()
    fun releaseResources()
}
