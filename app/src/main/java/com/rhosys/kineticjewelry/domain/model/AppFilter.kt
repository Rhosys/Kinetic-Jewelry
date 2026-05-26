package com.rhosys.kineticjewelry.domain.model

import android.graphics.drawable.Drawable

data class AppFilter(
    val packageName: String,
    val appLabel: String,
    val isWatched: Boolean,
    val vibrationMode: VibrationMode,
    val isContactLevelEnabled: Boolean,
    val iconDrawable: Drawable? = null,
)
