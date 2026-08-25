package ch.rhosys.lyra.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ch.rhosys.lyra.domain.model.AppFilter
import ch.rhosys.lyra.domain.model.VibrationMode

@Entity(tableName = "app_filters")
data class AppFilterEntity(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val isWatched: Boolean,
    @ColumnInfo(name = "vibration_mode_id") val vibrationModeId: Int,
    val isContactLevelEnabled: Boolean,
    @ColumnInfo(name = "has_call_category", defaultValue = "0") val hasCallCategory: Boolean = false,
    @ColumnInfo(name = "call_vibration_mode_id") val callVibrationModeId: Int? = null,
) {
    fun toDomain(): AppFilter = AppFilter(
        packageName = packageName,
        appLabel = appLabel,
        isWatched = isWatched,
        vibrationMode = VibrationMode.fromStableId(vibrationModeId),
        isContactLevelEnabled = isContactLevelEnabled,
        hasCallCategory = hasCallCategory,
        callVibrationMode = callVibrationModeId?.let { VibrationMode.fromStableId(it) },
    )

    companion object {
        fun fromDomain(filter: AppFilter) = AppFilterEntity(
            packageName = filter.packageName,
            appLabel = filter.appLabel,
            isWatched = filter.isWatched,
            vibrationModeId = filter.vibrationMode.stableId,
            isContactLevelEnabled = filter.isContactLevelEnabled,
            hasCallCategory = filter.hasCallCategory,
            callVibrationModeId = filter.callVibrationMode?.stableId,
        )
    }
}
