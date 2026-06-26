package ch.rhosys.lyra.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

private const val TAG = "VibrationListener"
private const val VIBRATE_PATH = "/kinetic/vibrate_raw"
private const val NOTIFICATION_CHANNEL_ID = "vibration_confirmation"
private const val NOTIFICATION_ID = 1001

/** Gap between repeat iterations — a real delay between separate vibrate() calls, never baked into a waveform. */
private const val REPEAT_GAP_MS = 200L

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
    private val vibrator: Vibrator
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }

    override fun onMessageReceived(event: MessageEvent) {
        Log.d(TAG, "onMessageReceived: path=${event.path}, ${event.data.size} byte(s)")
        if (event.path == VIBRATE_PATH) {
            playRawSequence(event.data)
        } else {
            Log.w(TAG, "Ignoring message on unknown path ${event.path}")
        }
    }

    /**
     * [data] is `[repeat, blockId, blockId, …]`. Each iteration is a separate [Vibrator.vibrate]
     * call; the gap between iterations is a real delay on this thread (WearableListenerService
     * callbacks run off the main thread), not a pause block folded into the waveform.
     */
    private fun playRawSequence(data: ByteArray) {
        if (data.isEmpty()) {
            Log.w(TAG, "playRawSequence: empty payload")
            showNotification("Vibration failed: empty payload")
            return
        }
        val repeatCount = data[0].toInt().coerceAtLeast(1)
        val blocks = data.drop(1).mapNotNull { BLOCK_DURATIONS[it] }
        if (blocks.isEmpty()) {
            Log.w(TAG, "playRawSequence: no recognized block ids in payload")
            showNotification("Vibration failed: no recognized blocks")
            return
        }

        val timings = blocks.map { it.first }.toLongArray()
        val amplitudes = blocks.map { it.second }.toIntArray()
        val waveform = VibrationEffect.createWaveform(timings, amplitudes, -1)
        val totalDurationMs = blocks.sumOf { it.first }
        Log.d(TAG, "playRawSequence: ${blocks.size} block(s), $repeatCount×, $totalDurationMs ms/cycle")

        val vibrator = vibrator
        try {
            for (i in 0 until repeatCount) {
                vibrator.vibrate(waveform)
                if (i < repeatCount - 1) Thread.sleep(totalDurationMs + REPEAT_GAP_MS)
            }
            showNotification("✓ Vibration sent (${blocks.size} blocks × $repeatCount)")
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed", e)
            showNotification("Vibration error: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun showNotification(message: String) {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Vibration Confirmations",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Confirms vibration commands received from phone"
                },
            )
        }

        val notification =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Lyra")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setOngoing(false)
                .setAutoCancel(true)
                .build()

        nm.notify(NOTIFICATION_ID, notification)
    }
}
