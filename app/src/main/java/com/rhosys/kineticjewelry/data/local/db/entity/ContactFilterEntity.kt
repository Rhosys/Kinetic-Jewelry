package com.rhosys.kineticjewelry.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.rhosys.kineticjewelry.domain.model.ContactFilter
import com.rhosys.kineticjewelry.domain.model.VibrationMode

@Entity(
    tableName = "contact_filters",
    primaryKeys = ["packageName", "groupName", "contactName"],
)
data class ContactFilterEntity(
    val packageName: String,
    val groupName: String,
    val contactName: String,
    val isWatched: Boolean?,
    @ColumnInfo(name = "vibration_mode_id") val vibrationModeId: Int?,
) {
    fun toDomain(): ContactFilter = ContactFilter(
        packageName = packageName,
        groupName = groupName,
        contactName = contactName,
        isWatched = isWatched,
        vibrationMode = vibrationModeId?.let { VibrationMode.fromStableId(it) },
    )

    companion object {
        fun fromDomain(filter: ContactFilter) = ContactFilterEntity(
            packageName = filter.packageName,
            groupName = filter.groupName,
            contactName = filter.contactName,
            isWatched = filter.isWatched,
            vibrationModeId = filter.vibrationMode?.stableId,
        )
    }
}
