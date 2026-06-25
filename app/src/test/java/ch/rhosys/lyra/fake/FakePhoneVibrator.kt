package ch.rhosys.lyra.fake

import ch.rhosys.lyra.domain.PhoneVibrator
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.VibrationBlock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePhoneVibrator : PhoneVibrator {
    private val noDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = noDevices.asStateFlow()
    override val connectedDevices: StateFlow<List<BluetoothDeviceInfo>> = noDevices.asStateFlow()
    override val scanResults: StateFlow<List<BluetoothDeviceInfo>> = noDevices.asStateFlow()
    override val isScanning: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    val sentCommands = mutableListOf<VibrationCommand>()

    override suspend fun sendVibration(
        address: String,
        blocks: List<VibrationBlock>,
        repeat: Int,
        timeoutMs: Long,
    ): Result<Unit> {
        sentCommands += VibrationCommand(address, blocks, repeat, System.currentTimeMillis())
        return Result.success(Unit)
    }

    override fun startScan() = Unit

    override fun stopScan() = Unit

    override fun refreshPairedDevices() = Unit

    override fun releaseResources() = Unit
}
