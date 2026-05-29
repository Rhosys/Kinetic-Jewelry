package ch.rhosys.lyra.di

import ch.rhosys.lyra.bluetooth.FakeBluetoothController
import ch.rhosys.lyra.domain.BluetoothController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DebugBluetoothModule {

    @Binds
    @Singleton
    abstract fun bindBluetoothController(impl: FakeBluetoothController): BluetoothController
}
