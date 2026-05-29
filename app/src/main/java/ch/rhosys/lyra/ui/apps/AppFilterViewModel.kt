package ch.rhosys.lyra.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.lyra.domain.model.AppFilter
import ch.rhosys.lyra.domain.model.ContactFilter
import ch.rhosys.lyra.domain.model.VibrationMode
import ch.rhosys.lyra.domain.repository.AppFilterRepository
import ch.rhosys.lyra.domain.repository.ContactFilterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppFilterViewModel @Inject constructor(
    private val appRepo: AppFilterRepository,
    private val contactRepo: ContactFilterRepository,
) : ViewModel() {

    val apps: StateFlow<List<AppFilter>> = appRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _expandedPackage = MutableStateFlow<String?>(null)
    val expandedPackage: StateFlow<String?> = _expandedPackage.asStateFlow()

    private val _contacts = MutableStateFlow<List<ContactFilter>>(emptyList())
    val contacts: StateFlow<List<ContactFilter>> = _contacts.asStateFlow()

    fun toggleExpanded(packageName: String) {
        val current = _expandedPackage.value
        if (current == packageName) {
            _expandedPackage.value = null
        } else {
            _expandedPackage.value = packageName
            viewModelScope.launch {
                contactRepo.observeByApp(packageName).collect { _contacts.value = it }
            }
        }
    }

    fun setAppWatched(filter: AppFilter, watched: Boolean) {
        viewModelScope.launch { appRepo.upsert(filter.copy(isWatched = watched)) }
    }

    fun setAppVibrationMode(filter: AppFilter, mode: VibrationMode) {
        viewModelScope.launch { appRepo.upsert(filter.copy(vibrationMode = mode)) }
    }

    fun setContactWatched(contact: ContactFilter, watched: Boolean?) {
        viewModelScope.launch { contactRepo.upsert(contact.copy(isWatched = watched)) }
    }

    fun setContactVibrationMode(contact: ContactFilter, mode: VibrationMode?) {
        viewModelScope.launch { contactRepo.upsert(contact.copy(vibrationMode = mode)) }
    }
}
