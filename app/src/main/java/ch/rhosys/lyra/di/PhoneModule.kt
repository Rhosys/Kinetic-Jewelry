package ch.rhosys.lyra.di

import ch.rhosys.lyra.data.phone.PhoneVibrationController
import ch.rhosys.lyra.domain.PhoneVibrator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PhoneModule {
    @Binds
    @Singleton
    abstract fun bindPhoneVibrator(impl: PhoneVibrationController): PhoneVibrator
}
