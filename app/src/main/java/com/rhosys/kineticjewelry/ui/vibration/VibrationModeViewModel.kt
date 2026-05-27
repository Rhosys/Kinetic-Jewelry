package com.rhosys.kineticjewelry.ui.vibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhosys.kineticjewelry.domain.BluetoothController
import com.rhosys.kineticjewelry.domain.model.BluetoothDeviceInfo
import com.rhosys.kineticjewelry.domain.model.VibrationMode
import com.rhosys.kineticjewelry.domain.repository.BluetoothDeviceRepository
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
