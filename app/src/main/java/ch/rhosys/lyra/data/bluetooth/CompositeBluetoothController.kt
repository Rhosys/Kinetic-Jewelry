package ch.rhosys.lyra.data.bluetooth

import ch.rhosys.lyra.data.wearos.WEAR_ADDRESS_PREFIX
import ch.rhosys.lyra.data.wearos.WearOsController
import ch.rhosys.lyra.domain.BluetoothController
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.VibrationMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompositeBluetoothController
    @Inject
    constructor(
        private val bleController: BluetoothControllerImpl,
        private val wearController: WearOsController,
    ) : BluetoothController {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        override val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> =
            combine(bleController.pairedDevices, wearController.wearNodes) { ble, wear -> ble + wear }
                .stateIn(scope, SharingStarted.Eagerly, emptyList())

        override val connectedDevices: StateFlow<List<BluetoothDeviceInfo>> =
            bleController.connectedDevices

        override val scanResults: StateFlow<List<BluetoothDeviceInfo>> =
            bleController.scanResults

        override val isScanning: StateFlow<Boolean> =
            bleController.isScanning

        override fun startScan() {
            bleController.startScan()
            scope.launch { wearController.refreshNodes() }
        }

        override fun stopScan() = bleController.stopScan()

        override fun refreshPairedDevices() {
            bleController.refreshPairedDevices()
            scope.launch { wearController.refreshNodes() }
        }

        override suspend fun sendVibration(
            address: String,
            mode: VibrationMode,
        ): Result<Unit> =
            if (address.startsWith(WEAR_ADDRESS_PREFIX)) {
                wearController.sendVibration(address.removePrefix(WEAR_ADDRESS_PREFIX), mode)
            } else {
                bleController.sendVibration(address, mode)
            }

        override fun releaseResources() = bleController.releaseResources()
    }
