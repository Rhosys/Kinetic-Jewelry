package ch.rhosys.lyra.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ch.rhosys.lyra.data.local.db.dao.AppFilterDao
import ch.rhosys.lyra.data.local.db.dao.BluetoothDeviceDao
import ch.rhosys.lyra.data.local.db.dao.ContactFilterDao
import ch.rhosys.lyra.data.local.db.dao.NotificationHistoryDao
import ch.rhosys.lyra.data.local.db.entity.AppFilterEntity
import ch.rhosys.lyra.data.local.db.entity.BluetoothDeviceEntity
import ch.rhosys.lyra.data.local.db.entity.ContactFilterEntity
import ch.rhosys.lyra.data.local.db.entity.NotificationHistoryEntity

@Database(
    entities = [AppFilterEntity::class, ContactFilterEntity::class, BluetoothDeviceEntity::class, NotificationHistoryEntity::class],
    version = 5,
    exportSchema = true,
)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appFilterDao(): AppFilterDao

    abstract fun contactFilterDao(): ContactFilterDao

    abstract fun bluetoothDeviceDao(): BluetoothDeviceDao

    abstract fun notificationHistoryDao(): NotificationHistoryDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `notification_history` " +
                            "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`packageName` TEXT NOT NULL, " +
                            "`appLabel` TEXT NOT NULL, " +
                            "`senderName` TEXT, " +
                            "`postedAt` INTEGER NOT NULL)",
                    )
                }
            }

        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `notification_history` ADD COLUMN `personIconUri` TEXT")
                }
            }

        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `bluetooth_devices` ADD COLUMN `device_type` TEXT NOT NULL DEFAULT 'BLE_JEWELRY'",
                    )
                }
            }

        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `bluetooth_devices` ADD COLUMN `connection_timeout_ms` INTEGER")
                    db.execSQL("ALTER TABLE `bluetooth_devices` ADD COLUMN `disabled_until` INTEGER")
                    db.execSQL("ALTER TABLE `bluetooth_devices` ADD COLUMN `consecutive_timeouts` INTEGER NOT NULL DEFAULT 0")
                }
            }
    }
}
