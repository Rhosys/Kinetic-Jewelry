package com.rhosys.kineticjewelry.data.notification

import com.rhosys.kineticjewelry.domain.model.NotificationEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<NotificationEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<NotificationEvent> = _events.asSharedFlow()

    private val _listenerConnected = MutableStateFlow(false)
    val listenerConnected: StateFlow<Boolean> = _listenerConnected.asStateFlow()

    fun emitEvent(event: NotificationEvent) {
        _events.tryEmit(event)
    }

    fun setListenerConnected(connected: Boolean) {
        _listenerConnected.value = connected
    }
}
