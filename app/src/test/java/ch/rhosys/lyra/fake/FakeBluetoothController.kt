package ch.rhosys.lyra.fake

import ch.rhosys.lyra.domain.BluetoothController
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.VibrationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeBluetoothController : BluetoothController {
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

    val sentCommands = mutableListOf<VibrationCommand>()

    override suspend fun sendVibration(address: String, mode: VibrationMode): Result<Unit> {
        sentCommands += VibrationCommand(address, mode, System.currentTimeMillis())
        return Result.success(Unit)
    }

    override fun releaseResources() = Unit
}
