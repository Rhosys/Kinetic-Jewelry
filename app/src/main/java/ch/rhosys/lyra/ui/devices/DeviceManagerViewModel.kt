package ch.rhosys.lyra.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.lyra.domain.BluetoothController
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.VibrationMode
import ch.rhosys.lyra.domain.repository.BluetoothDeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceManagerViewModel @Inject constructor(
    private val deviceRepo: BluetoothDeviceRepository,
    private val bluetoothController: BluetoothController,
) : ViewModel() {

    val alertDevices: StateFlow<List<BluetoothDeviceInfo>> = combine(
        deviceRepo.observeAlertEnabled(),
        bluetoothController.connectedDevices,
    ) { dbDevices, connected ->
        dbDevices.map { d ->
            connected.firstOrNull { it.address == d.address } ?: d
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = combine(
        bluetoothController.pairedDevices,
        deviceRepo.observeAlertEnabled(),
    ) { paired, alertEnabled ->
        val alertAddresses = alertEnabled.map { it.address }.toSet()
        paired.filter { it.address !in alertAddresses }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val scanResults: StateFlow<List<BluetoothDeviceInfo>> =
        bluetoothController.scanResults.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isScanning: StateFlow<Boolean> =
        bluetoothController.isScanning.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _snackbar = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    fun startScan() { bluetoothController.startScan() }
    fun stopScan() { bluetoothController.stopScan() }

    fun enableAlert(device: BluetoothDeviceInfo) {
        viewModelScope.launch { deviceRepo.upsert(device.copy(isAlertEnabled = true)) }
    }

    fun disableAlert(address: String) {
        viewModelScope.launch { deviceRepo.delete(address) }
    }

    fun testDevice(address: String) {
        viewModelScope.launch {
            val result = bluetoothController.sendVibration(address, VibrationMode.SHORT_PULSE)
            if (result.isFailure) {
                _snackbar.emit(result.exceptionOrNull()?.message ?: "Unknown error")
            } else {
                _snackbar.emit("Vibration sent")
            }
        }
    }
}
