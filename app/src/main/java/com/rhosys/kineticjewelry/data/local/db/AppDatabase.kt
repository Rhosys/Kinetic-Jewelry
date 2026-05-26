package com.rhosys.kineticjewelry.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rhosys.kineticjewelry.data.local.db.dao.AppFilterDao
import com.rhosys.kineticjewelry.data.local.db.dao.BluetoothDeviceDao
import com.rhosys.kineticjewelry.data.local.db.dao.ContactFilterDao
import com.rhosys.kineticjewelry.data.local.db.entity.AppFilterEntity
import com.rhosys.kineticjewelry.data.local.db.entity.BluetoothDeviceEntity
import com.rhosys.kineticjewelry.data.local.db.entity.ContactFilterEntity

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
