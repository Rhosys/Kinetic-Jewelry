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

    val sentCommands = mutableListOf<VibrationCommand>()

    override suspend fun sendVibration(address: String, mode: VibrationMode): Result<Unit> {
        sentCommands += VibrationCommand(address, mode, System.currentTimeMillis())
        return Result.success(Unit)
    }

    override fun releaseResources() = Unit
}
