package ch.rhosys.lyra.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.rhosys.lyra.domain.AppSettingsProvider
import ch.rhosys.lyra.domain.model.MultiDeviceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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

        // A corrupted or briefly unreadable Preferences file must never crash a collector —
        // fall back to defaults instead of propagating the read failure.
        private val safeData: Flow<Preferences> =
            dataStore.data.catch { emit(emptyPreferences()) }

        override val connectionTimeoutMs: Flow<Long> =
            safeData.map { prefs ->
                (prefs[KEY_CONNECTION_TIMEOUT_MS] ?: AppSettingsProvider.DEFAULT_TIMEOUT_MS)
                    .coerceIn(AppSettingsProvider.MIN_TIMEOUT_MS, AppSettingsProvider.SYSTEM_MAX_TIMEOUT_MS)
            }

        override val multiDeviceMode: Flow<MultiDeviceMode> =
            safeData.map { prefs ->
                runCatching { MultiDeviceMode.valueOf(prefs[KEY_MULTI_DEVICE_MODE] ?: "") }
                    .getOrDefault(MultiDeviceMode.ALL_DEVICES)
            }

        override val autoReEnable24h: Flow<Boolean> =
            safeData.map { prefs -> prefs[KEY_AUTO_RE_ENABLE_24H] ?: true }

        suspend fun setConnectionTimeoutMs(ms: Long) =
            dataStore.edit { prefs ->
                prefs[KEY_CONNECTION_TIMEOUT_MS] =
                    ms.coerceIn(AppSettingsProvider.MIN_TIMEOUT_MS, AppSettingsProvider.SYSTEM_MAX_TIMEOUT_MS)
            }

        suspend fun setMultiDeviceMode(mode: MultiDeviceMode) = dataStore.edit { prefs -> prefs[KEY_MULTI_DEVICE_MODE] = mode.name }

        suspend fun setAutoReEnable24h(enabled: Boolean) = dataStore.edit { prefs -> prefs[KEY_AUTO_RE_ENABLE_24H] = enabled }
    }
