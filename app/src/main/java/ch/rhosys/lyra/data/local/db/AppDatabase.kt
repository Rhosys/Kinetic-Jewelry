package ch.rhosys.lyra.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ch.rhosys.lyra.data.local.db.dao.AppFilterDao
import ch.rhosys.lyra.data.local.db.dao.BluetoothDeviceDao
import ch.rhosys.lyra.data.local.db.dao.ContactFilterDao
import ch.rhosys.lyra.data.local.db.entity.AppFilterEntity
import ch.rhosys.lyra.data.local.db.entity.BluetoothDeviceEntity
import ch.rhosys.lyra.data.local.db.entity.ContactFilterEntity

@Database(
    entities = [AppFilterEntity::class, ContactFilterEntity::class, BluetoothDeviceEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appFilterDao(): AppFilterDao
    abstract fun contactFilterDao(): ContactFilterDao
    abstract fun bluetoothDeviceDao(): BluetoothDeviceDao
}
