package ch.rhosys.lyra.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ch.rhosys.lyra.data.local.db.entity.BluetoothDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BluetoothDeviceDao {
    @Query("SELECT * FROM bluetooth_devices WHERE isAlertEnabled = 1")
    fun observeAlertEnabled(): Flow<List<BluetoothDeviceEntity>>

    @Query("SELECT * FROM bluetooth_devices")
    suspend fun getAll(): List<BluetoothDeviceEntity>

    @Query("SELECT * FROM bluetooth_devices WHERE address = :address LIMIT 1")
    suspend fun getByAddress(address: String): BluetoothDeviceEntity?

    @Upsert
    suspend fun upsert(entity: BluetoothDeviceEntity)

    @Query("DELETE FROM bluetooth_devices WHERE address = :address")
    suspend fun delete(address: String)

    @Query("UPDATE bluetooth_devices SET consecutive_timeouts = consecutive_timeouts + 1 WHERE address = :address")
    suspend fun incrementConsecutiveTimeouts(address: String)

    @Query("SELECT consecutive_timeouts FROM bluetooth_devices WHERE address = :address")
    suspend fun getConsecutiveTimeouts(address: String): Int

    @Query("UPDATE bluetooth_devices SET consecutive_timeouts = 0 WHERE address = :address")
    suspend fun resetConsecutiveTimeouts(address: String)

    @Query("UPDATE bluetooth_devices SET disabled_until = :disabledUntil WHERE address = :address")
    suspend fun setDisabledUntil(
        address: String,
        disabledUntil: Long?,
    )
}
