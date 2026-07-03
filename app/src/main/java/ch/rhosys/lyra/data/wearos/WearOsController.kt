package ch.rhosys.lyra.data.wearos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import ch.rhosys.lyra.data.AppLogger
import ch.rhosys.lyra.domain.AppSettingsProvider
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.ConnectionState
import ch.rhosys.lyra.domain.model.DeviceType
import ch.rhosys.lyra.domain.model.VibrationBlock
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

const val WEAR_ADDRESS_PREFIX = "wear:"
private const val VIBRATE_PATH = "/kinetic/vibrate_raw"
private const val CAPABILITY_VIBRATE = "lyra_vibrate"
private const val NOTIFICATION_CHANNEL_ID = "wear_vibration_mirror"
private const val NOTIFICATION_ID_BASE = 9000
private const val NOTIFICATION_CANCEL_DELAY_MS = 3_000L

@Singleton
class WearOsController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val logger: AppLogger,
    ) {
        private val _wearNodes = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
        val wearNodes: StateFlow<List<BluetoothDeviceInfo>> = _wearNodes.asStateFlow()

        private val handler = Handler(Looper.getMainLooper())

        suspend fun refreshNodes() {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                _wearNodes.value =
                    nodes.map { node ->
                        BluetoothDeviceInfo(
                            address = "$WEAR_ADDRESS_PREFIX${node.id}",
                            name = node.displayName,
                            isAlertEnabled = false,
                            connectionState = ConnectionState.CONNECTED,
                            deviceType = DeviceType.WEAR_OS,
                        )
                    }
                logger.info("Wear OS nodes refreshed: ${nodes.size} connected")
            } catch (e: Exception) {
                logger.error("Failed to refresh Wear OS nodes", e)
                _wearNodes.value = emptyList()
            }
        }

        /** Sends a vibration to the Wear OS node. Uses MessageClient if the wear app is installed, otherwise falls back to notification mirroring. */
        suspend fun sendVibration(
            nodeId: String,
            blocks: List<VibrationBlock>,
            repeat: Int,
            timeoutMs: Long = AppSettingsProvider.DEFAULT_TIMEOUT_MS,
        ): Result<Unit> {
            val hasWearApp = checkWearAppInstalled(nodeId)
            return if (hasWearApp) {
                logger.info("Wear app detected on $nodeId — using MessageClient")
                sendViaMessageClient(nodeId, blocks, repeat, timeoutMs)
            } else {
                logger.info("Wear app NOT detected on $nodeId — using notification mirroring")
                sendViaNotification(blocks, repeat)
            }
        }

        private suspend fun checkWearAppInstalled(nodeId: String): Boolean =
            try {
                val capabilityInfo = Wearable
                    .getCapabilityClient(context)
                    .getCapability(CAPABILITY_VIBRATE, CapabilityClient.FILTER_REACHABLE)
                    .await()
                capabilityInfo.nodes.any { it.id == nodeId }
            } catch (e: Exception) {
                logger.warn("Capability check failed: ${e.message}")
                false
            }

        private suspend fun sendViaMessageClient(
            nodeId: String,
            blocks: List<VibrationBlock>,
            repeat: Int,
            timeoutMs: Long,
        ): Result<Unit> =
            runCatching {
                withTimeout(timeoutMs) {
                    val payload = byteArrayOf(repeat.coerceAtLeast(1).toByte()) + blocks.map { it.id }.toByteArray()
                    logger.info("Wear sendMessage: nodeId=$nodeId, path=$VIBRATE_PATH, payload=${payload.size} bytes, timeout=${timeoutMs}ms")
                    val taskResult = Wearable
                        .getMessageClient(context)
                        .sendMessage(nodeId, VIBRATE_PATH, payload)
                        .await()
                    logger.info("Wear sendMessage completed: requestId=$taskResult")
                    Unit
                }
            }.also { result ->
                result.onSuccess { logger.info("Wear vibration sent to $nodeId ($blocks × $repeat)") }
                result.onFailure { e ->
                    logger.error("Wear vibration failed for $nodeId [${e.javaClass.simpleName}]", e)
                }
            }

        private fun sendViaNotification(
            blocks: List<VibrationBlock>,
            repeat: Int,
        ): Result<Unit> =
            runCatching {
                ensureNotificationChannel()

                val vibrationPattern = buildVibratePattern(blocks, repeat)
                logger.info("Posting mirror notification: ${vibrationPattern.size} timing entries, total ${vibrationPattern.sum()}ms")

                val notificationId = NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 100).toInt()
                val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Lyra")
                    .setContentText("\u2713")
                    .setVibrate(vibrationPattern)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .build()

                val nm = context.getSystemService(NotificationManager::class.java)
                nm.notify(notificationId, notification)

                // Auto-cancel after delay so it doesn't linger on phone
                handler.postDelayed({
                    nm.cancel(notificationId)
                }, NOTIFICATION_CANCEL_DELAY_MS)

                logger.info("Mirror notification posted (id=$notificationId), will auto-cancel in ${NOTIFICATION_CANCEL_DELAY_MS}ms")
            }.also { result ->
                result.onFailure { e ->
                    logger.error("Mirror notification failed [${e.javaClass.simpleName}]", e)
                }
            }

        private fun ensureNotificationChannel() {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Watch Vibrations",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Vibration alerts mirrored to your watch"
                    enableVibration(true)
                    setSound(null, null)
                }
                nm.createNotificationChannel(channel)
            }
        }

        /**
         * Converts VibrationBlock sequence to Android vibration pattern (wait, vibrate, wait, vibrate, ...).
         * The pattern starts with 0 (no initial wait), then alternates vibrate/pause durations.
         * Repeats are appended as additional cycles with a gap between them.
         */
        private fun buildVibratePattern(blocks: List<VibrationBlock>, repeat: Int): LongArray {
            val singleCycle = mutableListOf<Long>()
            // Android vibrate pattern: [wait, vibrate, wait, vibrate, ...]
            // First element is always the initial wait (0 = start immediately)
            singleCycle.add(0L)

            for (block in blocks) {
                if (block.motorOn) {
                    singleCycle.add(block.durationMs.toLong())
                } else {
                    // Pause block: add as wait duration
                    singleCycle.add(block.durationMs.toLong())
                }
            }

            val repeatCount = repeat.coerceAtLeast(1)
            if (repeatCount == 1) return singleCycle.toLongArray()

            // Multiple repeats: add a gap between cycles
            val result = mutableListOf<Long>()
            for (i in 0 until repeatCount) {
                if (i == 0) {
                    result.addAll(singleCycle)
                } else {
                    // Gap between repeats (200ms pause)
                    result.add(200L)
                    // Skip the leading 0 from singleCycle for subsequent repeats
                    result.addAll(singleCycle.drop(1))
                }
            }
            return result.toLongArray()
        }
    }
