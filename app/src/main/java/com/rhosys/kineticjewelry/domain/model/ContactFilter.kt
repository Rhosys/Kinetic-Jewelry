package com.rhosys.kineticjewelry.domain.model

data class ContactFilter(
    val packageName: String,
    val groupName: String,
    val contactName: String,
    val isWatched: Boolean?,
    val vibrationMode: VibrationMode?,
)
