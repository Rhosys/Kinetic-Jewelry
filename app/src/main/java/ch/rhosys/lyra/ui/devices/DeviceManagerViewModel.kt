package ch.rhosys.lyra.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.lyra.domain.BluetoothController
import ch.rhosys.lyra.domain.PhoneVibrator
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceManagerViewModel
    @Inject
    constructor(
        private val deviceRepo: BluetoothDeviceRepository,
        private val bluetoothController: BluetoothController,
        private val phoneVibrator: PhoneVibrator,
    ) : ViewModel() {
        /** Favorited devices, shown regardless of [BluetoothDeviceInfo.isAlertEnabled] — disabling a
         * favorite keeps it here rather than demoting it back to Recent Devices. */
        val favorites: StateFlow<List<BluetoothDeviceInfo>> =
            combine(
                deviceRepo.observeFavorites(),
                bluetoothController.connectedDevices,
            ) { dbDevices, connected ->
                dbDevices.map { d ->
                    connected.firstOrNull { it.address == d.address } ?: d
                }.sortedBy { it.name.lowercase() }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        /** Paired/known devices that haven't been favorited yet. */
        val recentDevices: StateFlow<List<BluetoothDeviceInfo>> =
            combine(
                bluetoothController.pairedDevices,
                deviceRepo.observeFavorites(),
            ) { paired, favorites ->
                val favoriteAddresses = favorites.map { it.address }.toSet()
                paired.filter { it.address !in favoriteAddresses }.sortedBy { it.name.lowercase() }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        val scanResults: StateFlow<List<BluetoothDeviceInfo>> =
            bluetoothController.scanResults
                .map { results -> results.sortedBy { it.name.lowercase() } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        val isScanning: StateFlow<Boolean> =
            bluetoothController.isScanning.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        private val _snackbar = MutableSharedFlow<String>(extraBufferCapacity = 4)
        val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

        fun startScan() {
            bluetoothController.startScan()
        }

        fun stopScan() {
            bluetoothController.stopScan()
        }

        init {
            bluetoothController.refreshPairedDevices()
        }

        /** Call after BLE permissions are granted to reload bonded devices. */
        fun refreshDevices() {
            bluetoothController.refreshPairedDevices()
        }

        /** Adds [device] to Favorites, immediately enabled. */
        fun addFavorite(device: BluetoothDeviceInfo) {
            viewModelScope.launch { deviceRepo.upsert(device.copy(isFavorite = true, isAlertEnabled = true)) }
        }

        /** Toggles whether a favorited device is active; re-enabling also clears any auto-disable window. */
        fun setEnabled(
            address: String,
            enabled: Boolean,
        ) {
            viewModelScope.launch { deviceRepo.setEnabled(address, enabled) }
        }

        fun removeFavorite(address: String) {
            viewModelScope.launch { deviceRepo.delete(address) }
        }

        fun testDevice(address: String) {
            viewModelScope.launch {
                // The phone is always available and isn't part of the alert-enabled device list —
                // it vibrates unconditionally here too, matching ProcessNotificationUseCase.
                phoneVibrator.sendVibration(PhoneVibrator.ADDRESS, VibrationMode.SHORT_PULSE.blocks)

                val result = bluetoothController.sendVibration(address, VibrationMode.SHORT_PULSE.blocks)
                if (result.isFailure) {
                    _snackbar.emit(result.exceptionOrNull()?.message ?: "Unknown error")
                } else {
                    _snackbar.emit("Vibration sent")
                }
            }
        }
    }
