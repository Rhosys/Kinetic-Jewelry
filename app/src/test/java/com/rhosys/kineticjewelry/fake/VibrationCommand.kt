package com.rhosys.kineticjewelry.fake

import com.rhosys.kineticjewelry.domain.model.VibrationMode

data class VibrationCommand(
    val address: String,
    val mode: VibrationMode,
    val sentAt: Long,
)
