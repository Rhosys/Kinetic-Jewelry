package ch.rhosys.lyra.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.lyra.domain.model.NotificationHistoryEntry
import ch.rhosys.lyra.domain.repository.NotificationHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NotificationHistoryViewModel @Inject constructor(
    repo: NotificationHistoryRepository,
) : ViewModel() {
    val entries: StateFlow<List<NotificationHistoryEntry>> = repo.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
