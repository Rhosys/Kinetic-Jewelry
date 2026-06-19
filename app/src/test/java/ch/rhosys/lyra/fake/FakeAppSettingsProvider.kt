package ch.rhosys.lyra.fake

import ch.rhosys.lyra.domain.AppSettingsProvider
import ch.rhosys.lyra.domain.model.MultiDeviceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAppSettingsProvider(
    timeoutMs: Long = AppSettingsProvider.DEFAULT_TIMEOUT_MS,
    mode: MultiDeviceMode = MultiDeviceMode.ALL_DEVICES,
    autoReEnable: Boolean = false,
) : AppSettingsProvider {
    override val connectionTimeoutMs: Flow<Long> = MutableStateFlow(timeoutMs)
    override val multiDeviceMode: Flow<MultiDeviceMode> = MutableStateFlow(mode)
    override val autoReEnable24h: Flow<Boolean> = MutableStateFlow(autoReEnable)
}
