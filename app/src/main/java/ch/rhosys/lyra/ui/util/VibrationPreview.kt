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
    // Use the amplitude-array overload for all modes — it's the most reliable
    // across OEMs. createOneShot with DEFAULT_AMPLITUDE is silently ignored on
    // some devices, and the timings-only createWaveform overload drops short
    // single-element patterns on others.
    // Timings alternate off/on starting with off; prepend 0ms so vibration
    // starts immediately. Amplitudes mirror: 0 for off slots, 255 for on slots.
    val timings = mutableListOf(0L)
    val amplitudes = mutableListOf(0)
    for (block in mode.blocks) {
        timings.add(block.durationMs.toLong())
        // Pause blocks (id >= 0xA) get amplitude 0; vibration blocks get max.
        amplitudes.add(if (block.id >= 0xA) 0 else 255)
    }
    return VibrationEffect.createWaveform(timings.toLongArray(), amplitudes.toIntArray(), -1)
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
