package com.rhosys.kineticjewelry.data.bluetooth

import android.bluetooth.BluetoothManager
import android.content.Context
import com.rhosys.kineticjewelry.domain.BluetoothController
import com.rhosys.kineticjewelry.domain.model.BluetoothDeviceInfo
import com.rhosys.kineticjewelry.domain.model.ConnectionState
import com.rhosys.kineticjewelry.domain.model.VibrationMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : BluetoothController {

    private val bluetoothAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val connectedDevices: StateFlow<List<BluetoothDeviceInfo>> = _connectedDevices.asStateFlow()

    fun refreshPairedDevices() {
        _pairedDevices.value = bluetoothAdapter.bondedDevices.map { device ->
            BluetoothDeviceInfo(
                address = device.address,
                name = device.name ?: device.address,
                isAlertEnabled = false,
                connectionState = ConnectionState.DISCONNECTED,
            )
        }
    }

    override suspend fun sendVibration(address: String, mode: VibrationMode): Result<Unit> {
        // Full BLE GATT implementation is in Task 9
        return Result.failure(NotImplementedError("BLE implementation pending"))
    }

    override fun releaseResources() = Unit
}
