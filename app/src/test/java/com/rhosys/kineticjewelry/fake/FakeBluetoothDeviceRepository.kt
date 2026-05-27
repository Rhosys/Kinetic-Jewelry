package com.rhosys.kineticjewelry.fake

import com.rhosys.kineticjewelry.domain.model.BluetoothDeviceInfo
import com.rhosys.kineticjewelry.domain.repository.BluetoothDeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeBluetoothDeviceRepository : BluetoothDeviceRepository {
    private val store = mutableMapOf<String, BluetoothDeviceInfo>()
    private val _flow = MutableStateFlow<Map<String, BluetoothDeviceInfo>>(emptyMap())

    override fun observeAlertEnabled(): Flow<List<BluetoothDeviceInfo>> =
        _flow.map { map -> map.values.filter { it.isAlertEnabled } }

    override suspend fun getAll(): List<BluetoothDeviceInfo> = store.values.toList()

    override suspend fun upsert(device: BluetoothDeviceInfo) {
        store[device.address] = device
        _flow.value = store.toMap()
    }

    override suspend fun delete(address: String) {
        store.remove(address)
        _flow.value = store.toMap()
    }
}
