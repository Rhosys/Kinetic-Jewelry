package ch.rhosys.lyra.ui.debug

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.lyra.domain.BluetoothController
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.VibrationBlock
import ch.rhosys.lyra.domain.model.VibrationMode
import ch.rhosys.lyra.domain.repository.BluetoothDeviceRepository
import ch.rhosys.lyra.ui.util.previewVibration
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugVibrationViewModel
    @Inject
    constructor(
        deviceRepo: BluetoothDeviceRepository,
        private val bluetoothController: BluetoothController,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        val alertDevices: StateFlow<List<BluetoothDeviceInfo>> = deviceRepo.observeAlertEnabled()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
                if (blocks.isEmpty()) {
                    _snackbar.emit("Sequence is empty — add at least one block")
                    return@launch
                }
                previewVibration(context, blocks, repeatCount)
                if (alertDevices.value.isEmpty()) {
                    _snackbar.emit("No device connected — add one in the Devices tab")
                    return@launch
                }
                alertDevices.value.forEach { device ->
                    val result = bluetoothController.sendRawVibration(device.address, blocks, repeatCount)
                    if (result.isFailure) {
                        _snackbar.emit(result.exceptionOrNull()?.message ?: "Unknown error")
                    }
                }
                _snackbar.emit("Sent to ${alertDevices.value.size} device(s)")
            }
        }
    }
