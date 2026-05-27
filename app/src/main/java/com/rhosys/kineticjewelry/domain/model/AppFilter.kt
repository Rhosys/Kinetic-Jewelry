package com.rhosys.kineticjewelry.domain.model

data class AppFilter(
    val packageName: String,
    val appLabel: String,
    val isWatched: Boolean,
    val vibrationMode: VibrationMode,
    val isContactLevelEnabled: Boolean,
)
