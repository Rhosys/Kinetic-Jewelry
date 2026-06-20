package ch.rhosys.lyra.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import ch.rhosys.lyra.domain.model.VibrationMode
import kotlinx.coroutines.delay

private const val TEST_REPEAT_DELAY_MS = 1000L

private fun vibratorOf(context: Context): Vibrator =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

private fun toWaveform(mode: VibrationMode): VibrationEffect {
    // A single block is just a continuous buzz; createOneShot is the API meant
    // for that and is honored consistently, whereas a 2-element createWaveform
    // array ([0, duration]) is silently dropped by some OEM vibrator HALs.
    if (mode.blocks.size == 1) {
        return VibrationEffect.createOneShot(mode.blocks[0].durationMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE)
    }
    // createWaveform(timings, repeat) alternates off/on starting with off.
    // Prepend 0ms so the pattern starts vibrating immediately, then each
    // block duration follows. All VibrationMode patterns already alternate
    // buzz→pause→buzz so the off/on assignment is always correct.
    val timings = longArrayOf(0L) + mode.blocks.map { it.durationMs.toLong() }.toLongArray()
    return VibrationEffect.createWaveform(timings, -1)
}

fun previewVibration(
    context: Context,
    mode: VibrationMode,
) {
    val vibrator = vibratorOf(context)
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(toWaveform(mode))
}

/**
 * Plays the pattern on the phone, waits a beat, then plays it again — mirroring how the
 * pattern repeats on the device so the user can feel what they're about to send.
 */
suspend fun previewVibrationTwice(
    context: Context,
    mode: VibrationMode,
) {
    val vibrator = vibratorOf(context)
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(toWaveform(mode))
    delay(mode.totalDurationMs + TEST_REPEAT_DELAY_MS)
    vibrator.vibrate(toWaveform(mode))
}
