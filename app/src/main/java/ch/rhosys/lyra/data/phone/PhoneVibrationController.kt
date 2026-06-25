package ch.rhosys.lyra.data.phone

import android.content.Context
import ch.rhosys.lyra.domain.PhoneVibrator
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.VibrationBlock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneVibrationController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PhoneVibrator {
        private val noDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
        override val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = noDevices.asStateFlow()
        override val connectedDevices: StateFlow<List<BluetoothDeviceInfo>> = noDevices.asStateFlow()
        override val scanResults: StateFlow<List<BluetoothDeviceInfo>> = noDevices.asStateFlow()
        override val isScanning: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

        override suspend fun sendVibration(
            address: String,
            blocks: List<VibrationBlock>,
            repeat: Int,
            timeoutMs: Long,
        ): Result<Unit> = runCatching { previewVibration(context, blocks, repeat) }

        override fun startScan() = Unit

        override fun stopScan() = Unit

        override fun refreshPairedDevices() = Unit

        override fun releaseResources() = Unit
    }
