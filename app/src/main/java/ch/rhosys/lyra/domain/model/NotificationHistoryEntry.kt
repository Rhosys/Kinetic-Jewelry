package ch.rhosys.lyra.domain.model

data class NotificationHistoryEntry(
    val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val senderName: String?,
    val postedAt: Long,
)
