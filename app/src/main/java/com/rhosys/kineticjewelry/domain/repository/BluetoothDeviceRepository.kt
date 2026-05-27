package com.rhosys.kineticjewelry.domain.repository

import com.rhosys.kineticjewelry.domain.model.BluetoothDeviceInfo
import kotlinx.coroutines.flow.Flow

interface BluetoothDeviceRepository {
    fun observeAlertEnabled(): Flow<List<BluetoothDeviceInfo>>
    suspend fun getAll(): List<BluetoothDeviceInfo>
    suspend fun upsert(device: BluetoothDeviceInfo)
    suspend fun delete(address: String)
}
