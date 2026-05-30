package ch.rhosys.lyra.di

import ch.rhosys.lyra.data.repository.AppFilterRepositoryImpl
import ch.rhosys.lyra.data.repository.BluetoothDeviceRepositoryImpl
import ch.rhosys.lyra.data.repository.ContactFilterRepositoryImpl
import ch.rhosys.lyra.data.repository.NotificationHistoryRepositoryImpl
import ch.rhosys.lyra.domain.repository.AppFilterRepository
import ch.rhosys.lyra.domain.repository.BluetoothDeviceRepository
import ch.rhosys.lyra.domain.repository.ContactFilterRepository
import ch.rhosys.lyra.domain.repository.NotificationHistoryRepository
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

    @Binds
    @Singleton
    abstract fun bindNotificationHistoryRepository(impl: NotificationHistoryRepositoryImpl): NotificationHistoryRepository
}
