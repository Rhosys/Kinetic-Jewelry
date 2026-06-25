package ch.rhosys.lyra.domain

import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.VibrationBlock
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>>
    val connectedDevices: StateFlow<List<BluetoothDeviceInfo>>
    val scanResults: StateFlow<List<BluetoothDeviceInfo>>
    val isScanning: StateFlow<Boolean>

    /** Sends a raw block sequence to the device at [address]. Callers convert a [ch.rhosys.lyra.domain.model.VibrationMode] to [blocks] themselves. */
    suspend fun sendVibration(
        address: String,
        blocks: List<VibrationBlock>,
        repeat: Int = 1,
        timeoutMs: Long = AppSettingsProvider.DEFAULT_TIMEOUT_MS,
    ): Result<Unit>

    fun startScan()

    fun stopScan()

    fun refreshPairedDevices()

    fun releaseResources()
}
