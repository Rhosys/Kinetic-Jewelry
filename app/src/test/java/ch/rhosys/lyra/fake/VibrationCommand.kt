package ch.rhosys.lyra.fake

import ch.rhosys.lyra.domain.model.VibrationMode

data class VibrationCommand(
    val address: String,
    val mode: VibrationMode,
    val sentAt: Long,
)
