package com.rhosys.kineticjewelry.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.rhosys.kineticjewelry.data.local.db.entity.BluetoothDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BluetoothDeviceDao {
    @Query("SELECT * FROM bluetooth_devices WHERE isAlertEnabled = 1")
    fun observeAlertEnabled(): Flow<List<BluetoothDeviceEntity>>

    @Query("SELECT * FROM bluetooth_devices")
    suspend fun getAll(): List<BluetoothDeviceEntity>

    @Upsert
    suspend fun upsert(entity: BluetoothDeviceEntity)

    @Query("DELETE FROM bluetooth_devices WHERE address = :address")
    suspend fun delete(address: String)
}
