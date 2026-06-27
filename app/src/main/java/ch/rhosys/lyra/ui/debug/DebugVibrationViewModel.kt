package ch.rhosys.lyra.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.lyra.data.AppLogger
import ch.rhosys.lyra.domain.BluetoothController
import ch.rhosys.lyra.domain.PhoneVibrator
import ch.rhosys.lyra.domain.model.VibrationBlock
import ch.rhosys.lyra.domain.model.VibrationMode
import ch.rhosys.lyra.domain.repository.BluetoothDeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Builds an arbitrary block sequence and sends it to the phone + all alert-enabled devices. */
@HiltViewModel
class DebugVibrationViewModel
    @Inject
    constructor(
        private val phoneVibrator: PhoneVibrator,
        private val bluetoothController: BluetoothController,
        private val deviceRepo: BluetoothDeviceRepository,
        private val logger: AppLogger,
    ) : ViewModel() {
        private val _sequence = MutableStateFlow<List<VibrationBlock>>(emptyList())
        val sequence: StateFlow<List<VibrationBlock>> = _sequence.asStateFlow()

        private val _repeat = MutableStateFlow(1)
        val repeat: StateFlow<Int> = _repeat.asStateFlow()

        private val _snackbar = MutableSharedFlow<String>(extraBufferCapacity = 4)
        val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

        fun addBlock(block: VibrationBlock) {
            _sequence.value = _sequence.value + block
        }

        fun removeBlockAt(index: Int) {
            _sequence.value = _sequence.value.toMutableList().apply { removeAt(index) }
        }

        fun clearSequence() {
            _sequence.value = emptyList()
        }

        fun setRepeat(value: Int) {
            _repeat.value = value.coerceIn(1, 9)
        }

        fun applyMode(mode: VibrationMode) {
            _sequence.value = mode.blocks
            _repeat.value = 1
        }

        fun vibrate() {
            val blocks = _sequence.value
            val repeatCount = _repeat.value
            viewModelScope.launch {
                if (blocks.size < 3) {
                    logger.warn("Debug vibration rejected: sequence has ${blocks.size} block(s), needs at least 3")
                    _snackbar.emit("Sequence must have at least 3 blocks")
                    return@launch
                }
                // Phone always vibrates
                phoneVibrator.sendVibration(PhoneVibrator.ADDRESS, blocks, repeatCount)

                // Also send to all alert-enabled devices
                val devices = deviceRepo.observeAlertEnabled().first()
                if (devices.isEmpty()) {
                    _snackbar.emit("Vibrated phone ($repeatCount×) — no alert devices registered")
                    return@launch
                }
                var anySuccess = false
                for (device in devices) {
                    val result = bluetoothController.sendVibration(device.address, blocks, repeatCount)
                    if (result.isSuccess) {
                        anySuccess = true
                    } else {
                        logger.error("Debug vibration failed for ${device.name} (${device.address}): ${result.exceptionOrNull()?.message}")
                    }
                }
                if (anySuccess) {
                    _snackbar.emit("Vibrated phone + ${devices.size} device(s) ($repeatCount×)")
                } else {
                    _snackbar.emit("Vibrated phone only — all ${devices.size} device(s) failed")
                }
            }
        }
    }
