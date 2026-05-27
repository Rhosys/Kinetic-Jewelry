package com.rhosys.kineticjewelry.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rhosys.kineticjewelry.domain.model.AppFilter
import com.rhosys.kineticjewelry.domain.model.VibrationMode

@Entity(tableName = "app_filters")
data class AppFilterEntity(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val isWatched: Boolean,
    @ColumnInfo(name = "vibration_mode_id") val vibrationModeId: Int,
    val isContactLevelEnabled: Boolean,
) {
    fun toDomain(): AppFilter = AppFilter(
        packageName = packageName,
        appLabel = appLabel,
        isWatched = isWatched,
        vibrationMode = VibrationMode.fromStableId(vibrationModeId),
        isContactLevelEnabled = isContactLevelEnabled,
    )

    companion object {
        fun fromDomain(filter: AppFilter) = AppFilterEntity(
            packageName = filter.packageName,
            appLabel = filter.appLabel,
            isWatched = filter.isWatched,
            vibrationModeId = filter.vibrationMode.stableId,
            isContactLevelEnabled = filter.isContactLevelEnabled,
        )
    }
}
