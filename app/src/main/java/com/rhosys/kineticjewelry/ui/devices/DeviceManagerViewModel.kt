package com.rhosys.kineticjewelry.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhosys.kineticjewelry.domain.BluetoothController
import com.rhosys.kineticjewelry.domain.model.BluetoothDeviceInfo
import com.rhosys.kineticjewelry.domain.model.VibrationMode
import com.rhosys.kineticjewelry.domain.repository.BluetoothDeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
        val connectedAddresses = connected.map { it.address }.toSet()
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

    fun enableAlert(device: BluetoothDeviceInfo) {
        viewModelScope.launch { deviceRepo.upsert(device.copy(isAlertEnabled = true)) }
    }

    fun disableAlert(address: String) {
        viewModelScope.launch { deviceRepo.delete(address) }
    }

    fun testDevice(address: String) {
        viewModelScope.launch {
            bluetoothController.sendVibration(address, VibrationMode.SHORT_PULSE)
        }
    }
}
