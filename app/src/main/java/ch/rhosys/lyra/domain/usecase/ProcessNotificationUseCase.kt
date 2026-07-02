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
            if (contactName.isNullOrBlank()) {
                logger.info("Notification skipped: contactName is blank for $packageName")
                return
            }

            val appFilter = appFilterRepository.getByPackageName(packageName)
            if (appFilter == null) {
                logger.info("Notification skipped: $packageName not in app filter list")
                return
            }
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
                        // Contact-level filtering is a whitelist ("All Users" is OFF): only senders
                        // and groups the user explicitly curated may vibrate. A sender with no rule —
                        // because they were never added, or were removed — must NOT fall back to the
                        // app default, otherwise removing a user would never stop their vibrations.
                        effectiveIsWatched = false
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
            val phoneResult = phoneVibrator.sendVibration(PhoneVibrator.ADDRESS, effectiveMode.blocks)
            if (phoneResult.isFailure) {
                logger.error("Phone vibration failed: ${phoneResult.exceptionOrNull()?.message}")
            }

            val allAlertDevices = bluetoothDeviceRepository.observeAlertEnabled().first()
            val userTimeoutMs = appSettings.connectionTimeoutMs.first()
            val multiDeviceMode = appSettings.multiDeviceMode.first()
            val autoReEnable = appSettings.autoReEnable24h.first()

            logger.info("Alert-enabled devices: ${allAlertDevices.size} (addresses: ${allAlertDevices.map { "${it.name}/${it.address}" }})")

            // Lazy auto-re-enable: bring back devices whose disable window has expired
            val now = System.currentTimeMillis()
            val activeDevices =
                allAlertDevices.mapNotNull { device ->
                    when {
                        !device.isCurrentlyDisabled -> device
                        autoReEnable && now >= (device.disabledUntil ?: 0) + AppSettingsProvider.AUTO_RE_ENABLE_DURATION_MS -> {
                            logger.info("Auto-re-enabling ${device.name} (${device.address}) — disable window expired")
                            bluetoothDeviceRepository.setDisabledUntil(device.address, null)
                            device.copy(disabledUntil = null)
                        }
                        else -> {
                            logger.info("Skipping disabled device ${device.name} (${device.address})")
                            null
                        }
                    }
                }

            if (activeDevices.isEmpty()) {
                logger.info("No active alert-enabled devices — phone-only vibration")
                return
            }

            logger.info("Dispatching to ${activeDevices.size} device(s) via $multiDeviceMode (timeout: ${userTimeoutMs}ms)")
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
                    logger.warn("ALL_DEVICES: all ${devices.size} device(s) failed — no ACK received")
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
