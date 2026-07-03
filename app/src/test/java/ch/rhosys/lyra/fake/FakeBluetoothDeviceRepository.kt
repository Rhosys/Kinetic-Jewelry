package ch.rhosys.lyra.fake

import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.repository.BluetoothDeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeBluetoothDeviceRepository : BluetoothDeviceRepository {
    private val store = mutableMapOf<String, BluetoothDeviceInfo>()
    private val deviceFlow: MutableStateFlow<Map<String, BluetoothDeviceInfo>> = MutableStateFlow(emptyMap())

    override fun observeFavorites(): Flow<List<BluetoothDeviceInfo>> = deviceFlow.map { map -> map.values.filter { it.isFavorite } }

    override suspend fun getAll(): List<BluetoothDeviceInfo> = store.values.toList()

    override suspend fun upsert(device: BluetoothDeviceInfo) {
        store[device.address] = device
        deviceFlow.value = store.toMap()
    }

    override suspend fun delete(address: String) {
        store.remove(address)
        deviceFlow.value = store.toMap()
    }

    override suspend fun recordSuccess(address: String) {
        val device = store[address] ?: return
        store[address] = device.copy(consecutiveTimeouts = 0, disabledUntil = null)
        deviceFlow.value = store.toMap()
    }

    override suspend fun recordFailure(address: String) {
        val device = store[address] ?: return
        val newCount = device.consecutiveTimeouts + 1
        val disabledUntil = if (newCount >= 2) System.currentTimeMillis() + 60 * 60 * 1_000L else null
        store[address] = device.copy(consecutiveTimeouts = newCount, disabledUntil = disabledUntil)
        deviceFlow.value = store.toMap()
    }

    override suspend fun setDisabledUntil(
        address: String,
        disabledUntil: Long?,
    ) {
        val device = store[address] ?: return
        store[address] = device.copy(disabledUntil = disabledUntil)
        deviceFlow.value = store.toMap()
    }

    override suspend fun setEnabled(
        address: String,
        enabled: Boolean,
    ) {
        val device = store[address] ?: return
        store[address] = device.copy(isAlertEnabled = enabled, disabledUntil = if (enabled) null else device.disabledUntil)
        deviceFlow.value = store.toMap()
    }
}
