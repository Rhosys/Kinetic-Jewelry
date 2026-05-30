package ch.rhosys.lyra.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.lyra.data.AppLogger
import ch.rhosys.lyra.data.LogEntry
import ch.rhosys.lyra.data.notification.NotificationEventBus
import ch.rhosys.lyra.service.KineticNotificationListenerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventBus: NotificationEventBus,
    private val logger: AppLogger,
) : ViewModel() {

    private val _listenerEnabled = MutableStateFlow(checkListenerEnabled())
    val listenerEnabled: StateFlow<Boolean> = _listenerEnabled.asStateFlow()

    val listenerConnected: StateFlow<Boolean> = eventBus.listenerConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val logEntries: StateFlow<List<LogEntry>> = logger.entries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _batteryOptimizationIgnored = MutableStateFlow(checkBatteryOptimization())
    val batteryOptimizationIgnored: StateFlow<Boolean> = _batteryOptimizationIgnored.asStateFlow()

    /** Call from onResume or LaunchedEffect to refresh after returning from Settings. */
    fun refreshStatus() {
        _listenerEnabled.value = checkListenerEnabled()
        _batteryOptimizationIgnored.value = checkBatteryOptimization()
    }

    private fun checkListenerEnabled(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    private fun checkBatteryOptimization(): Boolean =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(context.packageName)

    fun openNotificationListenerSettings(context: Context) {
        val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, KineticNotificationListenerService::class.java).flattenToString()
                )
            }
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openBatteryOptimizationSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun requestRebind() {
        android.service.notification.NotificationListenerService.requestRebind(
            ComponentName(context, KineticNotificationListenerService::class.java)
        )
    }

    fun clearLogs() { logger.clear() }
}
