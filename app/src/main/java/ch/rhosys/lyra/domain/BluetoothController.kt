package ch.rhosys.lyra.domain

import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.VibrationBlock
import ch.rhosys.lyra.domain.model.VibrationMode
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>>
    val connectedDevices: StateFlow<List<BluetoothDeviceInfo>>
    val scanResults: StateFlow<List<BluetoothDeviceInfo>>
    val isScanning: StateFlow<Boolean>

    suspend fun sendVibration(
        address: String,
        mode: VibrationMode,
        timeoutMs: Long = AppSettingsProvider.DEFAULT_TIMEOUT_MS,
    ): Result<Unit>

    /** Sends an arbitrary block sequence, bypassing the named [VibrationMode]s — debug/diagnostic use. */
    suspend fun sendRawVibration(
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
