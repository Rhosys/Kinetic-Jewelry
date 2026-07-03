package ch.rhosys.lyra.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.lyra.domain.model.AppFilter
import ch.rhosys.lyra.domain.model.ContactFilter
import ch.rhosys.lyra.domain.model.NotificationHistoryEntry
import ch.rhosys.lyra.domain.model.VibrationMode
import ch.rhosys.lyra.domain.repository.AppFilterRepository
import ch.rhosys.lyra.domain.repository.ContactFilterRepository
import ch.rhosys.lyra.domain.repository.NotificationHistoryRepository
import ch.rhosys.lyra.domain.usecase.DeviceVibrationDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppFilterViewModel
    @Inject
    constructor(
        private val appRepo: AppFilterRepository,
        private val contactRepo: ContactFilterRepository,
        private val historyRepo: NotificationHistoryRepository,
        private val dispatcher: DeviceVibrationDispatcher,
    ) : ViewModel() {
        val apps: StateFlow<List<AppFilter>> =
            appRepo
                .observeAll()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        val historyEntries: StateFlow<List<NotificationHistoryEntry>> =
            historyRepo
                .observeRecent()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        private val _contactsByApp = MutableStateFlow<Map<String, List<ContactFilter>>>(emptyMap())
        val contactsByApp: StateFlow<Map<String, List<ContactFilter>>> = _contactsByApp.asStateFlow()

        // Remove accordion state — no longer needed
        private val _expandedPackage = MutableStateFlow<String?>(null)
        val expandedPackage: StateFlow<String?> = _expandedPackage.asStateFlow()

        private val _contacts = MutableStateFlow<List<ContactFilter>>(emptyList())
        val contacts: StateFlow<List<ContactFilter>> = _contacts.asStateFlow()

        init {
            // Load contacts for all watched apps
            viewModelScope.launch {
                apps.collectLatest { appList ->
                    val map = mutableMapOf<String, List<ContactFilter>>()
                    for (app in appList) {
                        val contacts = contactRepo.getByApp(app.packageName)
                        map[app.packageName] = contacts
                    }
                    _contactsByApp.value = map
                }
            }
        }

        fun addApp(
            packageName: String,
            label: String,
        ) {
            viewModelScope.launch {
                val existing = appRepo.getByPackageName(packageName)
                if (existing == null) {
                    appRepo.upsert(
                        AppFilter(
                            packageName = packageName,
                            appLabel = label,
                            isWatched = true,
                            vibrationMode = VibrationMode.SHORT_PULSE,
                            isContactLevelEnabled = false,
                        ),
                    )
                }
            }
        }

        fun addUser(
            senderName: String,
            packageName: String,
            appLabel: String,
        ) {
            viewModelScope.launch {
                // Ensure app exists with contact-level enabled
                val existing = appRepo.getByPackageName(packageName)
                if (existing == null) {
                    appRepo.upsert(
                        AppFilter(
                            packageName = packageName,
                            appLabel = appLabel,
                            isWatched = true,
                            vibrationMode = VibrationMode.SHORT_PULSE,
                            isContactLevelEnabled = true,
                        ),
                    )
                } else if (!existing.isContactLevelEnabled) {
                    appRepo.upsert(existing.copy(isContactLevelEnabled = true))
                }

                // Add the contact as watched
                val existingContact = contactRepo.get(packageName, "", senderName)
                if (existingContact == null) {
                    contactRepo.upsert(
                        ContactFilter(
                            packageName = packageName,
                            groupName = "",
                            contactName = senderName,
                            isWatched = true,
                            vibrationMode = null,
                        ),
                    )
                }
                // Refresh contacts map
                refreshContacts(packageName)
            }
        }

        fun removeApp(packageName: String) {
            viewModelScope.launch {
                appRepo.delete(packageName)
            }
        }

        fun setContactLevelEnabled(
            filter: AppFilter,
            enabled: Boolean,
        ) {
            viewModelScope.launch { appRepo.upsert(filter.copy(isContactLevelEnabled = enabled)) }
        }

        fun removeContact(contact: ContactFilter) {
            viewModelScope.launch {
                contactRepo.delete(contact.packageName, contact.groupName, contact.contactName)
                refreshContacts(contact.packageName)
            }
        }

        fun setAppVibrationMode(
            packageName: String,
            mode: VibrationMode,
        ) {
            viewModelScope.launch {
                val existing = appRepo.getByPackageName(packageName) ?: return@launch
                appRepo.upsert(existing.copy(vibrationMode = mode))
            }
        }

        fun setContactVibrationMode(
            contact: ContactFilter,
            mode: VibrationMode?,
        ) {
            viewModelScope.launch {
                contactRepo.upsert(contact.copy(vibrationMode = mode))
                refreshContacts(contact.packageName)
            }
        }

        /** Plays [mode] on the phone and every favorited, enabled device so the user can feel it while picking a mode. */
        fun demoVibration(mode: VibrationMode) {
            viewModelScope.launch { dispatcher.dispatch(mode.blocks) }
        }

        private suspend fun refreshContacts(packageName: String) {
            val contacts = contactRepo.getByApp(packageName)
            _contactsByApp.value = _contactsByApp.value.toMutableMap().apply { put(packageName, contacts) }
        }
    }
