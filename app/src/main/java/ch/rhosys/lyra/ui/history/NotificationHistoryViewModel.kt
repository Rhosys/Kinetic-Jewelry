package ch.rhosys.lyra.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.lyra.domain.model.AppFilter
import ch.rhosys.lyra.domain.model.ContactFilter
import ch.rhosys.lyra.domain.model.NotificationHistoryEntry
import ch.rhosys.lyra.domain.model.VibrationMode
import ch.rhosys.lyra.domain.repository.AppFilterRepository
import ch.rhosys.lyra.domain.repository.ContactFilterRepository
import ch.rhosys.lyra.domain.repository.NotificationHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationHistoryViewModel @Inject constructor(
    repo: NotificationHistoryRepository,
    private val appFilterRepo: AppFilterRepository,
    private val contactFilterRepo: ContactFilterRepository,
) : ViewModel() {

    val entries: StateFlow<List<NotificationHistoryEntry>> = repo.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun watchUser(entry: NotificationHistoryEntry) {
        val senderName = entry.senderName
        viewModelScope.launch {
            // Ensure app is in the watch list
            val existing = appFilterRepo.getByPackageName(entry.packageName)
            if (existing == null) {
                appFilterRepo.upsert(
                    AppFilter(
                        packageName = entry.packageName,
                        appLabel = entry.appLabel,
                        isWatched = true,
                        vibrationMode = VibrationMode.SHORT_PULSE,
                        isContactLevelEnabled = !senderName.isNullOrBlank(),
                    )
                )
            } else if (!senderName.isNullOrBlank() && !existing.isContactLevelEnabled) {
                appFilterRepo.upsert(existing.copy(isContactLevelEnabled = true))
            }

            // Add the contact as watched (if there's a sender name)
            if (!senderName.isNullOrBlank()) {
                val existingContact = contactFilterRepo.get(entry.packageName, "", senderName)
                if (existingContact == null) {
                    contactFilterRepo.upsert(
                        ContactFilter(
                            packageName = entry.packageName,
                            groupName = "",
                            contactName = senderName,
                            isWatched = true,
                            vibrationMode = null,
                        )
                    )
                }
            }
        }
    }
}
