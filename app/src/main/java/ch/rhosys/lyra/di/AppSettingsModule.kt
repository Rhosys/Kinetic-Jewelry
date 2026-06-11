package ch.rhosys.lyra.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import ch.rhosys.lyra.data.settings.AppSettings
import ch.rhosys.lyra.domain.AppSettingsProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppSettingsModule {
    @Binds
    @Singleton
    abstract fun bindAppSettingsProvider(impl: AppSettings): AppSettingsProvider

    companion object {
        @Provides
        @Singleton
        fun provideAppSettingsDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                produceFile = { File(context.filesDir, "app_settings.preferences_pb") },
            )
    }
}
