package ch.rhosys.lyra.di

import android.content.Context
import androidx.room.Room
import ch.rhosys.lyra.data.local.db.AppDatabase
import ch.rhosys.lyra.data.local.db.dao.AppFilterDao
import ch.rhosys.lyra.data.local.db.dao.BluetoothDeviceDao
import ch.rhosys.lyra.data.local.db.dao.ContactFilterDao
import ch.rhosys.lyra.data.local.db.dao.NotificationHistoryDao
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
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, "kinetic_jewelry.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
            )
            .build()

    @Provides
    fun provideAppFilterDao(db: AppDatabase): AppFilterDao = db.appFilterDao()

    @Provides
    fun provideContactFilterDao(db: AppDatabase): ContactFilterDao = db.contactFilterDao()

    @Provides
    fun provideBluetoothDeviceDao(db: AppDatabase): BluetoothDeviceDao = db.bluetoothDeviceDao()

    @Provides
    fun provideNotificationHistoryDao(db: AppDatabase): NotificationHistoryDao = db.notificationHistoryDao()
}
