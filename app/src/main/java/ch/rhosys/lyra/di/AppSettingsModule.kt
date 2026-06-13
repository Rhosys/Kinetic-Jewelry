package ch.rhosys.lyra.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import ch.rhosys.lyra.data.settings.AppSettings
import ch.rhosys.lyra.domain.AppSettingsProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

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
        ): DataStore<Preferences> = context.appSettingsDataStore
    }
}
