package ch.rhosys.lyra.domain.repository

import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import kotlinx.coroutines.flow.Flow

interface BluetoothDeviceRepository {
    /** All favorited devices, regardless of [BluetoothDeviceInfo.isAlertEnabled]. */
    fun observeFavorites(): Flow<List<BluetoothDeviceInfo>>

    suspend fun getAll(): List<BluetoothDeviceInfo>

    suspend fun upsert(device: BluetoothDeviceInfo)

    suspend fun delete(address: String)

    suspend fun recordSuccess(address: String)

    suspend fun recordFailure(address: String)

    suspend fun setDisabledUntil(
        address: String,
        disabledUntil: Long?,
    )

    /** Manually toggles whether a favorited device is active. Enabling also clears any auto-disable window. */
    suspend fun setEnabled(
        address: String,
        enabled: Boolean,
    )
}
