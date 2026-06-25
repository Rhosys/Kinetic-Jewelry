package ch.rhosys.lyra.data.bluetooth

import ch.rhosys.lyra.data.AppLogger
import ch.rhosys.lyra.data.wearos.WEAR_ADDRESS_PREFIX
import ch.rhosys.lyra.data.wearos.WearOsController
import ch.rhosys.lyra.domain.BluetoothController
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.VibrationBlock
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
        private val logger: AppLogger,
    ) : BluetoothController {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        override val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> =
            combine(bleController.pairedDevices, wearController.wearNodes, ::excludeWearOsDuplicates)
                .stateIn(scope, SharingStarted.Eagerly, emptyList())

        override val connectedDevices: StateFlow<List<BluetoothDeviceInfo>> =
            combine(bleController.connectedDevices, wearController.wearNodes, ::excludeWearOsDuplicates)
                .stateIn(scope, SharingStarted.Eagerly, emptyList())

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
            blocks: List<VibrationBlock>,
            repeat: Int,
            timeoutMs: Long,
        ): Result<Unit> =
            if (address.startsWith(WEAR_ADDRESS_PREFIX)) {
                val nodeId = address.removePrefix(WEAR_ADDRESS_PREFIX)
                logger.info("Routing vibration to Wear OS node $nodeId")
                wearController.sendVibration(nodeId, blocks, repeat, timeoutMs)
            } else {
                logger.info("Routing vibration to BLE device $address")
                bleController.sendVibration(address, blocks, repeat, timeoutMs)
            }

        override fun releaseResources() = bleController.releaseResources()
    }

/**
 * The Wear OS API and the classic Bluetooth bonded-device list both surface the same physical
 * watch, but with unrelated identifiers (a Wearable node id vs. a Bluetooth MAC address) — there's
 * no shared key to match them on. Display name is the only field both sides agree on, so a BLE
 * entry is treated as the watch's raw radio link and dropped whenever a Wear OS node already
 * reports that name.
 */
private fun excludeWearOsDuplicates(
    ble: List<BluetoothDeviceInfo>,
    wear: List<BluetoothDeviceInfo>,
): List<BluetoothDeviceInfo> {
    val wearNames = wear.map { it.name.lowercase() }.toSet()
    return ble.filter { it.name.lowercase() !in wearNames } + wear
}
