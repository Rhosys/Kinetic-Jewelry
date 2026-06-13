package ch.rhosys.lyra.data.wearos

import android.content.Context
import ch.rhosys.lyra.data.AppLogger
import ch.rhosys.lyra.domain.AppSettingsProvider
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.ConnectionState
import ch.rhosys.lyra.domain.model.DeviceType
import ch.rhosys.lyra.domain.model.VibrationMode
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
private const val VIBRATE_PATH = "/kinetic/vibrate"

@Singleton
class WearOsController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val logger: AppLogger,
    ) {
        private val _wearNodes = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
        val wearNodes: StateFlow<List<BluetoothDeviceInfo>> = _wearNodes.asStateFlow()

        suspend fun refreshNodes() {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                _wearNodes.value =
                    nodes.map { node ->
                        BluetoothDeviceInfo(
                            address = "$WEAR_ADDRESS_PREFIX${node.id}",
                            name = node.displayName,
                            isAlertEnabled = false,
                            connectionState = ConnectionState.DISCONNECTED,
                            deviceType = DeviceType.WEAR_OS,
                        )
                    }
                logger.info("Wear OS nodes refreshed: ${nodes.size} connected")
            } catch (e: Exception) {
                logger.error("Failed to refresh Wear OS nodes", e)
                _wearNodes.value = emptyList()
            }
        }

        suspend fun sendVibration(
            nodeId: String,
            mode: VibrationMode,
            timeoutMs: Long = AppSettingsProvider.DEFAULT_TIMEOUT_MS,
        ): Result<Unit> =
            runCatching {
                withTimeout(timeoutMs) {
                    val payload = byteArrayOf(mode.stableId.toByte())
                    Wearable
                        .getMessageClient(context)
                        .sendMessage(nodeId, VIBRATE_PATH, payload)
                        .await()
                    Unit
                }
            }.also { result ->
                result.onSuccess { logger.info("Wear vibration sent to $nodeId (${mode.displayName})") }
                result.onFailure { logger.error("Wear vibration failed for $nodeId", it) }
            }
    }
