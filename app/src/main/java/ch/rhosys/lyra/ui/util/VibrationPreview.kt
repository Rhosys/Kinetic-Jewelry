package ch.rhosys.lyra.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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

    if (!vibrator.hasVibrator()) return

    // createWaveform(timings, repeat) alternates off/on starting with off.
    // Prepend 0ms so the pattern starts vibrating immediately, then each
    // block duration follows. All VibrationMode patterns already alternate
    // buzz→pause→buzz so the off/on assignment is always correct.
    val timings = longArrayOf(0L) + mode.blocks.map { it.durationMs.toLong() }.toLongArray()
    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
}
