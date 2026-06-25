package ch.rhosys.lyra.wear

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

private const val VIBRATE_PATH = "/kinetic/vibrate"
private const val VIBRATE_RAW_PATH = "/kinetic/vibrate_raw"
private const val REPEAT_GAP_MS = 200L

// Mirrors VibrationMode/VibrationBlock on the phone — stableId → (timings, amplitudes)
private val WAVEFORMS: Map<Int, Pair<LongArray, IntArray>> =
    mapOf(
        1 to (longArrayOf(100, 80, 100, 80) to intArrayOf(200, 0, 200, 0)), // SHORT_PULSE
        2 to (longArrayOf(500, 200, 500, 200) to intArrayOf(200, 0, 200, 0)), // LONG_PULSE
        3 to (longArrayOf(40, 80, 40) to intArrayOf(200, 0, 200)), // DOUBLE_TAP
        4 to (longArrayOf(100, 80, 250, 600) to intArrayOf(200, 0, 200, 0)), // HEARTBEAT
        5 to (
            longArrayOf(40, 80, 100, 80, 250, 80, 500) // ESCALATING
                to intArrayOf(200, 0, 200, 0, 200, 0, 200)
        ),
        6 to ( // SOS
            longArrayOf(40, 80, 40, 80, 40, 200, 100, 80, 100, 80, 100, 200, 40, 80, 40, 80, 40, 600)
                to intArrayOf(200, 0, 200, 0, 200, 0, 200, 0, 200, 0, 200, 0, 200, 0, 200, 0, 200, 0)
        ),
    )

// Mirrors VibrationBlock on the phone — block id → (durationMs, amplitude)
private val BLOCK_DURATIONS: Map<Byte, Pair<Long, Int>> =
    mapOf(
        0x1.toByte() to (40L to 200), // CLICK
        0x2.toByte() to (100L to 200), // SHORT_BUZZ
        0x3.toByte() to (250L to 200), // MEDIUM_BUZZ
        0x4.toByte() to (500L to 200), // LONG_BUZZ
        0x5.toByte() to (1000L to 200), // EXTRA_LONG_BUZZ
        0xA.toByte() to (80L to 0), // SHORT_PAUSE
        0xB.toByte() to (200L to 0), // MEDIUM_PAUSE
        0xC.toByte() to (600L to 0), // LONG_PAUSE
    )

class VibrationListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        val waveform =
            when (event.path) {
                VIBRATE_PATH -> WAVEFORMS[event.data.firstOrNull()?.toInt() ?: return]
                VIBRATE_RAW_PATH -> decodeRawWaveform(event.data)
                else -> null
            } ?: return

        val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }
        vibrator.vibrate(VibrationEffect.createWaveform(waveform.first, waveform.second, -1))
    }

    /** [data] is `[repeat, blockId, blockId, …]`; iterations are joined by a medium pause. */
    private fun decodeRawWaveform(data: ByteArray): Pair<LongArray, IntArray>? {
        if (data.isEmpty()) return null
        val repeatCount = data[0].toInt().coerceAtLeast(1)
        val blocks = data.drop(1).mapNotNull { BLOCK_DURATIONS[it] }
        if (blocks.isEmpty()) return null

        val timings = mutableListOf<Long>()
        val amplitudes = mutableListOf<Int>()
        repeat(repeatCount) { i ->
            if (i > 0) {
                timings += REPEAT_GAP_MS
                amplitudes += 0
            }
            blocks.forEach { (duration, amplitude) ->
                timings += duration
                amplitudes += amplitude
            }
        }
        return timings.toLongArray() to amplitudes.toIntArray()
    }
}
