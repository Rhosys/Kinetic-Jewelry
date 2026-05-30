package ch.rhosys.lyra.bluetooth

import ch.rhosys.lyra.domain.BluetoothController
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.VibrationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class VibrationCommand(
    val address: String,
    val mode: VibrationMode,
    val sentAt: Long,
)

@Singleton
class FakeBluetoothController @Inject constructor() : BluetoothController {

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val connectedDevices: StateFlow<List<BluetoothDeviceInfo>> = _connectedDevices.asStateFlow()

    private val _scanResults = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val scanResults: StateFlow<List<BluetoothDeviceInfo>> = _scanResults.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    override fun startScan() { _isScanning.value = true }
    override fun stopScan() { _isScanning.value = false }
    override fun refreshPairedDevices() {}

    private val _log = MutableStateFlow<List<VibrationCommand>>(emptyList())
    val log: StateFlow<List<VibrationCommand>> = _log.asStateFlow()

    override suspend fun sendVibration(address: String, mode: VibrationMode): Result<Unit> {
        _log.value = _log.value + VibrationCommand(address, mode, System.currentTimeMillis())
        return Result.success(Unit)
    }

    fun clearLog() {
        _log.value = emptyList()
    }

    override fun releaseResources() = Unit
}
