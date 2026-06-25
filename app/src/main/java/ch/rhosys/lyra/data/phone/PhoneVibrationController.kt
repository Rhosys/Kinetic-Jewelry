package ch.rhosys.lyra.data.phone

import android.content.Context
import ch.rhosys.lyra.domain.BluetoothController
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.ConnectionState
import ch.rhosys.lyra.domain.model.DeviceType
import ch.rhosys.lyra.domain.model.VibrationBlock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Stable address for the phone's own vibrator, analogous to [ch.rhosys.lyra.data.wearos.WEAR_ADDRESS_PREFIX]. */
const val PHONE_ADDRESS = "phone:this-device"

/**
 * Third [BluetoothController] implementation, alongside BLE jewelry and Wear OS — the phone is
 * always "connected" and has nothing to scan for, so the discovery surface is constant/no-op.
 */
@Singleton
class PhoneVibrationController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : BluetoothController {
        private val phoneDevice =
            BluetoothDeviceInfo(
                address = PHONE_ADDRESS,
                name = "This Phone",
                isAlertEnabled = false,
                connectionState = ConnectionState.CONNECTED,
                deviceType = DeviceType.PHONE,
            )

        override val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> =
            MutableStateFlow(listOf(phoneDevice)).asStateFlow()

        override val connectedDevices: StateFlow<List<BluetoothDeviceInfo>> =
            MutableStateFlow(listOf(phoneDevice)).asStateFlow()

        override val scanResults: StateFlow<List<BluetoothDeviceInfo>> =
            MutableStateFlow(emptyList<BluetoothDeviceInfo>()).asStateFlow()

        override val isScanning: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

        override fun startScan() = Unit

        override fun stopScan() = Unit

        override fun refreshPairedDevices() = Unit

        override fun releaseResources() = Unit

        override suspend fun sendVibration(
            address: String,
            blocks: List<VibrationBlock>,
            repeat: Int,
            timeoutMs: Long,
        ): Result<Unit> = runCatching { previewVibration(context, blocks, repeat) }
    }
