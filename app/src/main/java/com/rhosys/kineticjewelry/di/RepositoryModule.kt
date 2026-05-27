package com.rhosys.kineticjewelry.di

import com.rhosys.kineticjewelry.data.repository.AppFilterRepositoryImpl
import com.rhosys.kineticjewelry.data.repository.BluetoothDeviceRepositoryImpl
import com.rhosys.kineticjewelry.data.repository.ContactFilterRepositoryImpl
import com.rhosys.kineticjewelry.domain.repository.AppFilterRepository
import com.rhosys.kineticjewelry.domain.repository.BluetoothDeviceRepository
import com.rhosys.kineticjewelry.domain.repository.ContactFilterRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppFilterRepository(impl: AppFilterRepositoryImpl): AppFilterRepository

    @Binds
    @Singleton
    abstract fun bindContactFilterRepository(impl: ContactFilterRepositoryImpl): ContactFilterRepository

    @Binds
    @Singleton
    abstract fun bindBluetoothDeviceRepository(impl: BluetoothDeviceRepositoryImpl): BluetoothDeviceRepository
}
