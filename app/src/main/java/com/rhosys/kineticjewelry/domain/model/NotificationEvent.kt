package com.rhosys.kineticjewelry.domain.model

data class NotificationEvent(
    val packageName: String,
    val senderName: String?,
    val text: String?,
    val category: String?,
    val postedAt: Long = System.currentTimeMillis(),
)
