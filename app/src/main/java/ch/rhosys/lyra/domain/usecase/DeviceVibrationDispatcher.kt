package ch.rhosys.lyra.domain.usecase

import ch.rhosys.lyra.data.AppLogger
import ch.rhosys.lyra.domain.AppSettingsProvider
import ch.rhosys.lyra.domain.BluetoothController
import ch.rhosys.lyra.domain.PhoneVibrator
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.MultiDeviceMode
import ch.rhosys.lyra.domain.model.VibrationBlock
import ch.rhosys.lyra.domain.repository.BluetoothDeviceRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

/**
 * Sends a block sequence to the phone and to every active (favorited + enabled) device, honoring
 * the user's multi-device dispatch setting and per-device timeouts. This is the single delivery
 * path for a resolved vibration — both real notification handling and the debug vibration screen
 * call it, so a debug test exercises exactly the same devices and dispatch logic as production.
 */
class DeviceVibrationDispatcher
    @Inject
    constructor(
        private val bluetoothDeviceRepository: BluetoothDeviceRepository,
        private val bluetoothController: BluetoothController,
        private val phoneVibrator: PhoneVibrator,
        private val appSettings: AppSettingsProvider,
        private val logger: AppLogger,
    ) {
        suspend fun dispatch(
            blocks: List<VibrationBlock>,
            repeat: Int = 1,
        ) {
            // The phone is always available and isn't part of the alert-enabled device list —
            // it vibrates unconditionally, independent of whatever BLE/Wear devices are configured.
            phoneVibrator.sendVibration(PhoneVibrator.ADDRESS, blocks, repeat)

            val allFavorites = bluetoothDeviceRepository.observeFavorites().first()
            val userTimeoutMs = appSettings.connectionTimeoutMs.first()
            val multiDeviceMode = appSettings.multiDeviceMode.first()
            val autoReEnable = appSettings.autoReEnable24h.first()

            // Lazy auto-re-enable: bring back devices whose disable window has expired
            val now = System.currentTimeMillis()
            val activeDevices =
                allFavorites.filter { it.isAlertEnabled }.mapNotNull { device ->
                    when {
                        !device.isCurrentlyDisabled -> device
                        autoReEnable && now >= (device.disabledUntil ?: 0) + AppSettingsProvider.AUTO_RE_ENABLE_DURATION_MS -> {
                            bluetoothDeviceRepository.setDisabledUntil(device.address, null)
                            device.copy(disabledUntil = null)
                        }
                        else -> null
                    }
                }

            if (activeDevices.isEmpty()) {
                logger.info("No active favorite devices — phone-only vibration")
                return
            }

            logger.info("Dispatching to ${activeDevices.size} device(s) via $multiDeviceMode")
            when (multiDeviceMode) {
                MultiDeviceMode.FIRST_WINS -> dispatchFirstWins(activeDevices, blocks, repeat, userTimeoutMs)
                MultiDeviceMode.ALL_DEVICES -> dispatchAllDevices(activeDevices, blocks, repeat, userTimeoutMs)
            }
        }

        private suspend fun dispatchFirstWins(
            devices: List<BluetoothDeviceInfo>,
            blocks: List<VibrationBlock>,
            repeat: Int,
            userTimeoutMs: Long,
        ) {
            val sorted = devices.sortedByDescending { it.lastSuccessAt ?: 0L }
            var succeeded = false
            for (device in sorted) {
                val timeoutMs = effectiveTimeout(device, userTimeoutMs)
                val result = bluetoothController.sendVibration(device.address, blocks, repeat, timeoutMs)
                if (result.isSuccess) {
                    bluetoothDeviceRepository.recordSuccess(device.address)
                    succeeded = true
                    break
                } else {
                    bluetoothDeviceRepository.recordFailure(device.address)
                }
            }
            if (!succeeded) logger.warn("FIRST_WINS: all ${sorted.size} device(s) failed")
        }

        private suspend fun dispatchAllDevices(
            devices: List<BluetoothDeviceInfo>,
            blocks: List<VibrationBlock>,
            repeat: Int,
            userTimeoutMs: Long,
        ) {
            supervisorScope {
                val firstAck = CompletableDeferred<Unit>()
                val jobs =
                    devices.map { device ->
                        val timeoutMs = effectiveTimeout(device, userTimeoutMs)
                        launch {
                            val result = bluetoothController.sendVibration(device.address, blocks, repeat, timeoutMs)
                            // Skip recording if this coroutine was cancelled (post-ACK window expired)
                            if (isActive) {
                                if (result.isSuccess) {
                                    firstAck.complete(Unit)
                                    bluetoothDeviceRepository.recordSuccess(device.address)
                                } else {
                                    bluetoothDeviceRepository.recordFailure(device.address)
                                }
                            }
                        }
                    }
                // Sentinel: unblock firstAck if every device failed without success
                launch {
                    jobs.forEach { it.join() }
                    firstAck.completeExceptionally(NoSuchElementException())
                }
                try {
                    firstAck.await()
                    delay(1_000L)
                } catch (_: Exception) {
                    // All devices failed — nothing to cancel
                }
                jobs.forEach { it.cancel() }
            }
        }

        private fun effectiveTimeout(
            device: BluetoothDeviceInfo,
            userTimeoutMs: Long,
        ): Long {
            val deviceCap = device.connectionTimeoutMs ?: Long.MAX_VALUE
            return minOf(userTimeoutMs, AppSettingsProvider.SYSTEM_MAX_TIMEOUT_MS, deviceCap)
        }
    }
