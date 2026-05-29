package ch.rhosys.lyra.ui.vibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.lyra.domain.BluetoothController
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.VibrationMode
import ch.rhosys.lyra.domain.repository.BluetoothDeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VibrationModeViewModel @Inject constructor(
    private val deviceRepo: BluetoothDeviceRepository,
    private val bluetoothController: BluetoothController,
) : ViewModel() {

    val alertDevices: StateFlow<List<BluetoothDeviceInfo>> = deviceRepo.observeAlertEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun testMode(mode: VibrationMode) {
        viewModelScope.launch {
            alertDevices.value.forEach { device ->
                bluetoothController.sendVibration(device.address, mode)
            }
        }
    }
}
