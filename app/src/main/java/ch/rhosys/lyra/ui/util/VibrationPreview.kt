package ch.rhosys.lyra.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import ch.rhosys.lyra.domain.model.VibrationBlock
import ch.rhosys.lyra.domain.model.VibrationMode
import ch.rhosys.lyra.domain.model.withoutTrailingPauses
import kotlinx.coroutines.delay

/** Gap between repeat iterations — kept out of the waveform itself, see [previewVibration]. */
private const val REPEAT_GAP_MS = 200L

private fun vibratorOf(context: Context): Vibrator =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

private fun toWaveform(blocks: List<VibrationBlock>): VibrationEffect {
    val timings = blocks.map { it.durationMs.toLong() }.toLongArray()
    val amplitudes = blocks.map { if (it.motorOn) VibrationEffect.DEFAULT_AMPLITUDE else 0 }.toIntArray()
    return VibrationEffect.createWaveform(timings, amplitudes, -1)
}

suspend fun previewVibration(
    context: Context,
    mode: VibrationMode,
) = previewVibration(context, mode.blocks)

/**
 * Plays a block sequence on the phone's own vibrator, repeated `repeat` times. The pause
 * between iterations is a real delay between separate [Vibrator.vibrate] calls, not a block
 * baked into the waveform — that way the waveform itself never has to encode anything other
 * than the mode's own alternating buzz/pause blocks.
 */
suspend fun previewVibration(
    context: Context,
    blocks: List<VibrationBlock>,
    repeat: Int = 1,
) {
    if (blocks.isEmpty()) return
    val vibrator = vibratorOf(context)
    if (!vibrator.hasVibrator()) return
    // The waveform itself must never end in dead time — a trailing pause is
    // either an authoring mistake or a leftover from the old baked-in repeat
    // gap, neither of which belongs in a single play-through.
    val effectiveBlocks = blocks.withoutTrailingPauses().ifEmpty { blocks }
    val waveform = toWaveform(effectiveBlocks)
    val totalDurationMs = effectiveBlocks.sumOf { it.durationMs }.toLong()
    val repeatCount = repeat.coerceAtLeast(1)
    for (i in 0 until repeatCount) {
        vibrator.vibrate(waveform)
        if (i < repeatCount - 1) delay(totalDurationMs + REPEAT_GAP_MS)
    }
}

