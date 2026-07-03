package ch.rhosys.lyra.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.lyra.data.AppLogger
import ch.rhosys.lyra.domain.model.VibrationBlock
import ch.rhosys.lyra.domain.model.VibrationMode
import ch.rhosys.lyra.domain.usecase.DeviceVibrationDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Builds an arbitrary block sequence and sends it through the exact same delivery path as a real
 * notification (phone + every favorited, enabled device, honoring the multi-device dispatch
 * setting) — the only difference from production is that the block sequence is picked explicitly
 * here instead of resolved from an app/contact's configured [VibrationMode].
 */
@HiltViewModel
class DebugVibrationViewModel
    @Inject
    constructor(
        private val dispatcher: DeviceVibrationDispatcher,
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
                dispatcher.dispatch(blocks, repeatCount)
                _snackbar.emit("Vibrated phone + enabled devices ($repeatCount×)")
            }
        }
    }
