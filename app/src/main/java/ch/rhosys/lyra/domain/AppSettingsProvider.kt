package ch.rhosys.lyra.domain

import ch.rhosys.lyra.domain.model.MultiDeviceMode
import kotlinx.coroutines.flow.Flow

interface AppSettingsProvider {
    companion object {
        const val SYSTEM_MAX_TIMEOUT_MS = 30_000L
        const val MIN_TIMEOUT_MS = 3_000L
        const val DEFAULT_TIMEOUT_MS = 10_000L
        const val AUTO_RE_ENABLE_DURATION_MS = 24 * 60 * 60 * 1_000L
    }

    val connectionTimeoutMs: Flow<Long>
    val multiDeviceMode: Flow<MultiDeviceMode>
    val autoReEnable24h: Flow<Boolean>
}
