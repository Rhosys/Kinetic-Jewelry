package ch.rhosys.lyra.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ch.rhosys.lyra.domain.model.NotificationHistoryEntry

@Entity(tableName = "notification_history")
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val senderName: String?,
    val personIconUri: String?,
    val postedAt: Long,
) {
    fun toDomain() = NotificationHistoryEntry(id, packageName, appLabel, senderName, personIconUri, postedAt)
    companion object {
        fun fromDomain(e: NotificationHistoryEntry) = NotificationHistoryEntity(e.id, e.packageName, e.appLabel, e.senderName, e.personIconUri, e.postedAt)
    }
}
