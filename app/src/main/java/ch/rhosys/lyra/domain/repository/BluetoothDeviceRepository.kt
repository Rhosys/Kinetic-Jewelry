package ch.rhosys.lyra.domain.repository

import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import kotlinx.coroutines.flow.Flow

interface BluetoothDeviceRepository {
    fun observeAlertEnabled(): Flow<List<BluetoothDeviceInfo>>

    suspend fun getAll(): List<BluetoothDeviceInfo>

    suspend fun upsert(device: BluetoothDeviceInfo)

    suspend fun delete(address: String)

    suspend fun recordSuccess(address: String)

    suspend fun recordFailure(address: String)

    suspend fun setDisabledUntil(
        address: String,
        disabledUntil: Long?,
    )
}
