package ch.rhosys.lyra.domain.usecase

import ch.rhosys.lyra.domain.BluetoothController
import ch.rhosys.lyra.domain.model.ContactFilter
import ch.rhosys.lyra.domain.model.VibrationMode
import ch.rhosys.lyra.domain.repository.AppFilterRepository
import ch.rhosys.lyra.domain.repository.BluetoothDeviceRepository
import ch.rhosys.lyra.domain.repository.ContactFilterRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ProcessNotificationUseCase @Inject constructor(
    private val appFilterRepository: AppFilterRepository,
    private val contactFilterRepository: ContactFilterRepository,
    private val bluetoothDeviceRepository: BluetoothDeviceRepository,
    private val bluetoothController: BluetoothController,
) {

    suspend fun execute(
        packageName: String,
        groupName: String,
        contactName: String?,
    ) {
        if (contactName.isNullOrBlank()) return

        val appFilter = appFilterRepository.getByPackageName(packageName) ?: return
        if (!appFilter.isWatched) return

        val effectiveMode: VibrationMode

        if (!appFilter.isContactLevelEnabled) {
            effectiveMode = appFilter.vibrationMode
        } else {
            autoUpsertContact(packageName, groupName, contactName)

            val senderRule = contactFilterRepository.get(packageName, groupName, contactName)
            val groupRule = if (groupName.isNotEmpty())
                contactFilterRepository.get(packageName, groupName, "") else null

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

            if (!effectiveIsWatched) return
            effectiveMode = modeOverride ?: appFilter.vibrationMode
        }

        val alertDevices = bluetoothDeviceRepository.observeAlertEnabled().first()
        alertDevices.forEach { device ->
            bluetoothController.sendVibration(device.address, effectiveMode)
        }
    }

    private suspend fun autoUpsertContact(packageName: String, groupName: String, contactName: String) {
        if (contactFilterRepository.get(packageName, groupName, contactName) == null) {
            contactFilterRepository.upsert(
                ContactFilter(
                    packageName = packageName,
                    groupName = groupName,
                    contactName = contactName,
                    isWatched = null,
                    vibrationMode = null,
                )
            )
        }
    }
}
