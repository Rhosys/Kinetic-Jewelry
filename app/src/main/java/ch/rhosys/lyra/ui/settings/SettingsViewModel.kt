package ch.rhosys.lyra.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.rhosys.lyra.data.AppLogger
import ch.rhosys.lyra.data.LogEntry
import ch.rhosys.lyra.data.notification.NotificationEventBus
import ch.rhosys.lyra.data.settings.AppSettings
import ch.rhosys.lyra.domain.AppSettingsProvider
import ch.rhosys.lyra.domain.model.MultiDeviceMode
import ch.rhosys.lyra.service.KineticNotificationListenerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SettingsViewModel"

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val eventBus: NotificationEventBus,
        private val logger: AppLogger,
        private val appSettings: AppSettings,
    ) : ViewModel() {
        init {
            Log.d(TAG, "SettingsViewModel created")
        }

        // Anything that throws while the Settings tab is loading lands here instead of
        // crashing the app — the screen observes this and shows a copyable error dialog.
        private val _loadError = MutableStateFlow<Throwable?>(null)
        val loadError: StateFlow<Throwable?> = _loadError.asStateFlow()

        private val _listenerEnabled = MutableStateFlow(safeCheckListenerEnabled())
        val listenerEnabled: StateFlow<Boolean> = _listenerEnabled.asStateFlow()

        val listenerConnected: StateFlow<Boolean> =
            eventBus.listenerConnected
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        val logEntries: StateFlow<List<LogEntry>> =
            logger.entries
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        private val _batteryOptimizationIgnored = MutableStateFlow(safeCheckBatteryOptimization())
        val batteryOptimizationIgnored: StateFlow<Boolean> = _batteryOptimizationIgnored.asStateFlow()

        val connectionTimeoutMs: StateFlow<Long> =
            appSettings.connectionTimeoutMs
                .catch { e -> _loadError.value = e; emit(AppSettingsProvider.DEFAULT_TIMEOUT_MS) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsProvider.DEFAULT_TIMEOUT_MS)

        val multiDeviceMode: StateFlow<MultiDeviceMode> =
            appSettings.multiDeviceMode
                .catch { e -> _loadError.value = e; emit(MultiDeviceMode.ALL_DEVICES) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MultiDeviceMode.ALL_DEVICES)

        val autoReEnable24h: StateFlow<Boolean> =
            appSettings.autoReEnable24h
                .catch { e -> _loadError.value = e; emit(true) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

        fun refreshStatus() {
            _listenerEnabled.value = safeCheckListenerEnabled()
            _batteryOptimizationIgnored.value = safeCheckBatteryOptimization()
        }

        fun clearLoadError() {
            _loadError.value = null
        }

        private fun safeCheckListenerEnabled(): Boolean =
            try {
                checkListenerEnabled()
            } catch (e: Throwable) {
                Log.e(TAG, "safeCheckListenerEnabled failed", e)
                _loadError.value = e
                false
            }

        private fun safeCheckBatteryOptimization(): Boolean =
            try {
                checkBatteryOptimization()
            } catch (e: Throwable) {
                Log.e(TAG, "safeCheckBatteryOptimization failed", e)
                _loadError.value = e
                false
            }

        private fun checkListenerEnabled(): Boolean =
            NotificationManagerCompat
                .getEnabledListenerPackages(context)
                .contains(context.packageName)

        private fun checkBatteryOptimization(): Boolean =
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .isIgnoringBatteryOptimizations(context.packageName)

        fun openNotificationListenerSettings(context: Context) {
            val intent =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                        putExtra(
                            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                            ComponentName(context, KineticNotificationListenerService::class.java).flattenToString(),
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
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }

        fun requestRebind() {
            android.service.notification.NotificationListenerService.requestRebind(
                ComponentName(context, KineticNotificationListenerService::class.java),
            )
        }

        fun clearLogs() {
            logger.clear()
        }

        fun setConnectionTimeoutMs(ms: Long) {
            viewModelScope.launch { appSettings.setConnectionTimeoutMs(ms) }
        }

        fun setMultiDeviceMode(mode: MultiDeviceMode) {
            viewModelScope.launch { appSettings.setMultiDeviceMode(mode) }
        }

        fun setAutoReEnable24h(enabled: Boolean) {
            viewModelScope.launch { appSettings.setAutoReEnable24h(enabled) }
        }
    }
