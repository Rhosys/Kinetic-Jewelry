package ch.rhosys.lyra.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.rhosys.lyra.domain.AppSettingsProvider
import ch.rhosys.lyra.domain.model.MultiDeviceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettings
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : AppSettingsProvider {
        companion object {
            private val KEY_CONNECTION_TIMEOUT_MS = longPreferencesKey("connection_timeout_ms")
            private val KEY_MULTI_DEVICE_MODE = stringPreferencesKey("multi_device_mode")
            private val KEY_AUTO_RE_ENABLE_24H = booleanPreferencesKey("auto_re_enable_24h")
        }

        override val connectionTimeoutMs: Flow<Long> =
            dataStore.data.map { prefs ->
                (prefs[KEY_CONNECTION_TIMEOUT_MS] ?: AppSettingsProvider.DEFAULT_TIMEOUT_MS)
                    .coerceIn(AppSettingsProvider.MIN_TIMEOUT_MS, AppSettingsProvider.SYSTEM_MAX_TIMEOUT_MS)
            }

        override val multiDeviceMode: Flow<MultiDeviceMode> =
            dataStore.data.map { prefs ->
                runCatching { MultiDeviceMode.valueOf(prefs[KEY_MULTI_DEVICE_MODE] ?: "") }
                    .getOrDefault(MultiDeviceMode.ALL_DEVICES)
            }

        override val autoReEnable24h: Flow<Boolean> =
            dataStore.data.map { prefs -> prefs[KEY_AUTO_RE_ENABLE_24H] ?: false }

        suspend fun setConnectionTimeoutMs(ms: Long) =
            dataStore.edit { prefs ->
                prefs[KEY_CONNECTION_TIMEOUT_MS] =
                    ms.coerceIn(AppSettingsProvider.MIN_TIMEOUT_MS, AppSettingsProvider.SYSTEM_MAX_TIMEOUT_MS)
            }

        suspend fun setMultiDeviceMode(mode: MultiDeviceMode) = dataStore.edit { prefs -> prefs[KEY_MULTI_DEVICE_MODE] = mode.name }

        suspend fun setAutoReEnable24h(enabled: Boolean) = dataStore.edit { prefs -> prefs[KEY_AUTO_RE_ENABLE_24H] = enabled }
    }
