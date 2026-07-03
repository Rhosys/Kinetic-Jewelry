package ch.rhosys.lyra.domain.usecase

import ch.rhosys.lyra.data.AppLogger
import ch.rhosys.lyra.domain.model.VibrationMode
import ch.rhosys.lyra.domain.repository.AppFilterRepository
import ch.rhosys.lyra.domain.repository.ContactFilterRepository
import javax.inject.Inject

class ProcessNotificationUseCase
    @Inject
    constructor(
        private val appFilterRepository: AppFilterRepository,
        private val contactFilterRepository: ContactFilterRepository,
        private val dispatcher: DeviceVibrationDispatcher,
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
            dispatcher.dispatch(effectiveMode.blocks)
        }
    }
