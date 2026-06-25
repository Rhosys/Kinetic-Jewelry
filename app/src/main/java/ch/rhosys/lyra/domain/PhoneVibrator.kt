package ch.rhosys.lyra.domain

import ch.rhosys.lyra.domain.model.VibrationBlock

/**
 * The phone's own vibrator. Unlike [BluetoothController] devices it is never paired, scanned
 * for, or enabled/disabled by the user — it's always available, so every trigger (realtime
 * notification, debug screen, settings demo) calls it directly and explicitly.
 */
interface PhoneVibrator {
    suspend fun sendVibration(
        blocks: List<VibrationBlock>,
        repeat: Int = 1,
    ): Result<Unit>
}
