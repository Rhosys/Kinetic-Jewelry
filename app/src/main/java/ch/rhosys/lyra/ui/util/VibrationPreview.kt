package ch.rhosys.lyra.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import ch.rhosys.lyra.domain.model.VibrationBlock
import ch.rhosys.lyra.domain.model.VibrationMode

fun previewVibration(
    context: Context,
    mode: VibrationMode,
) {
    val vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    val timings = mode.blocks.map { it.durationMs.toLong() }.toLongArray()
    val amplitudes =
        mode.blocks
            .map { block ->
                when (block) {
                    VibrationBlock.SHORT_BUZZ, VibrationBlock.MEDIUM_BUZZ,
                    VibrationBlock.LONG_BUZZ, VibrationBlock.CLICK,
                    -> 200
                    VibrationBlock.SHORT_PAUSE, VibrationBlock.MEDIUM_PAUSE,
                    VibrationBlock.LONG_PAUSE,
                    -> 0
                }
            }.toIntArray()

    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
}
