package ch.rhosys.lyra.domain.usecase

import ch.rhosys.lyra.data.AppLogger
import ch.rhosys.lyra.domain.AppSettingsProvider
import ch.rhosys.lyra.domain.BluetoothController
import ch.rhosys.lyra.domain.PhoneVibrator
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.MultiDeviceMode
import ch.rhosys.lyra.domain.model.VibrationMode
import ch.rhosys.lyra.domain.repository.AppFilterRepository
import ch.rhosys.lyra.domain.repository.BluetoothDeviceRepository
import ch.rhosys.lyra.domain.repository.ContactFilterRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class ProcessNotificationUseCase
    @Inject
    constructor(
        private val appFilterRepository: AppFilterRepository,
        private val contactFilterRepository: ContactFilterRepository,
        private val bluetoothDeviceRepository: BluetoothDeviceRepository,
        private val bluetoothController: BluetoothController,
        private val phoneVibrator: PhoneVibrator,
        private val appSettings: AppSettingsProvider,
        private val logger: AppLogger,
    ) {
        suspend fun execute(
            packageName: String,
            groupName: String,
            contactName: String?,
        ) {
            if (contactName.isNullOrBlank()) return

            val appFilter = appFilterRepository.getByPackageName(packageName) ?: return
            if (!appFilter.isWatched) {
                logger.info("Notification ignored: $packageName is not watched")
                return
            }

            val effectiveMode: VibrationMode

            if (!appFilter.isContactLevelEnabled) {
                effectiveMode = appFilter.vibrationMode
            } else {
                val senderRule = contactFilterRepository.get(packageName, groupName, contactName)
                val groupRule =
                    if (groupName.isNotEmpty()) {
                        contactFilterRepository.get(packageName, groupName, "")
                    } else {
                        null
                    }

                val effectiveIsWatched: Boolean
                val modeOverride: VibrationMode?

                when {
                    senderRule?.isWatched != null -> {
                        effectiveIsWatched = senderRule.isWatched!!
                        modeOverride = senderRule.vibrationMode ?: groupRule?.vibrationMode
                    }
                    groupRule?.isWatched != null -> {
                        effectiveIsWatched = groupRule.isWatched!!
                        modeOverride = groupRule.vibrationMode
                    }
                    else -> {
                        effectiveIsWatched = appFilter.isWatched
                        modeOverride = null
                    }
                }

                if (!effectiveIsWatched) {
                    logger.info("Notification ignored: $packageName/$contactName is not watched")
                    return
                }
                effectiveMode = modeOverride ?: appFilter.vibrationMode
            }

            logger.info("Notification matched: $packageName/$contactName → ${effectiveMode.displayName}")

            // The phone is always available and isn't part of the alert-enabled device list —
            // it vibrates unconditionally, independent of whatever BLE/Wear devices are configured.
            phoneVibrator.sendVibration(PhoneVibrator.ADDRESS, effectiveMode.blocks)

            val allAlertDevices = bluetoothDeviceRepository.observeAlertEnabled().first()
            val userTimeoutMs = appSettings.connectionTimeoutMs.first()
            val multiDeviceMode = appSettings.multiDeviceMode.first()
            val autoReEnable = appSettings.autoReEnable24h.first()

            // Lazy auto-re-enable: bring back devices whose disable window has expired
            val now = System.currentTimeMillis()
            val activeDevices =
                allAlertDevices.mapNotNull { device ->
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
                logger.info("No active alert-enabled devices — phone-only vibration")
                return
            }

            logger.info("Dispatching to ${activeDevices.size} device(s) via $multiDeviceMode")
            when (multiDeviceMode) {
                MultiDeviceMode.FIRST_WINS -> dispatchFirstWins(activeDevices, effectiveMode, userTimeoutMs)
                MultiDeviceMode.ALL_DEVICES -> dispatchAllDevices(activeDevices, effectiveMode, userTimeoutMs)
            }
        }

        private suspend fun dispatchFirstWins(
            devices: List<BluetoothDeviceInfo>,
            mode: VibrationMode,
            userTimeoutMs: Long,
        ) {
            val sorted = devices.sortedByDescending { it.lastSuccessAt ?: 0L }
            var succeeded = false
            for (device in sorted) {
                val timeoutMs = effectiveTimeout(device, userTimeoutMs)
                val result = bluetoothController.sendVibration(device.address, mode.blocks, timeoutMs = timeoutMs)
                if (result.isSuccess) {
                    bluetoothDeviceRepository.recordSuccess(device.address)
                    succeeded = true
                    break
                } else {
                    logger.error("Device ${device.name} (${device.address}) failed: ${result.exceptionOrNull()?.message}")
                    bluetoothDeviceRepository.recordFailure(device.address)
                }
            }
            if (!succeeded) logger.warn("FIRST_WINS: all ${sorted.size} device(s) failed")
        }

        private suspend fun dispatchAllDevices(
            devices: List<BluetoothDeviceInfo>,
            mode: VibrationMode,
            userTimeoutMs: Long,
        ) {
            supervisorScope {
                val firstAck = CompletableDeferred<Unit>()
                val jobs =
                    devices.map { device ->
                        val timeoutMs = effectiveTimeout(device, userTimeoutMs)
                        launch {
                            val result = bluetoothController.sendVibration(device.address, mode.blocks, timeoutMs = timeoutMs)
                            // Skip recording if this coroutine was cancelled (post-ACK window expired)
                            if (isActive) {
                                if (result.isSuccess) {
                                    firstAck.complete(Unit)
                                    bluetoothDeviceRepository.recordSuccess(device.address)
                                } else {
                                    logger.error("Device ${device.name} (${device.address}) failed: ${result.exceptionOrNull()?.message}")
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
