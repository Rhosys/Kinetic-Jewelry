package ch.rhosys.lyra.wear

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

private const val VIBRATE_PATH = "/kinetic/vibrate"

// Mirrors VibrationMode/VibrationBlock on the phone — stableId → (timings, amplitudes)
private val WAVEFORMS: Map<Int, Pair<LongArray, IntArray>> =
    mapOf(
        1 to (longArrayOf(100) to intArrayOf(200)), // SHORT_PULSE
        2 to (longArrayOf(500) to intArrayOf(200)), // LONG_PULSE
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

class VibrationListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != VIBRATE_PATH) return
        val modeId = event.data.firstOrNull()?.toInt() ?: return
        val (timings, amplitudes) = WAVEFORMS[modeId] ?: return

        val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }
}
