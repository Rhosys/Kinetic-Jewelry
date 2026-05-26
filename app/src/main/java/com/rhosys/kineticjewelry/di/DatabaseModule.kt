package com.rhosys.kineticjewelry.di

import android.content.Context
import androidx.room.Room
import com.rhosys.kineticjewelry.data.local.db.AppDatabase
import com.rhosys.kineticjewelry.data.local.db.dao.AppFilterDao
import com.rhosys.kineticjewelry.data.local.db.dao.BluetoothDeviceDao
import com.rhosys.kineticjewelry.data.local.db.dao.ContactFilterDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "kinetic_jewelry.db")
            .build()

    @Provides
    fun provideAppFilterDao(db: AppDatabase): AppFilterDao = db.appFilterDao()

    @Provides
    fun provideContactFilterDao(db: AppDatabase): ContactFilterDao = db.contactFilterDao()

    @Provides
    fun provideBluetoothDeviceDao(db: AppDatabase): BluetoothDeviceDao = db.bluetoothDeviceDao()
}
