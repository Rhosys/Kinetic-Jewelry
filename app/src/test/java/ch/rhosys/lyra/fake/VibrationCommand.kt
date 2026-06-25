package ch.rhosys.lyra.fake

import ch.rhosys.lyra.domain.model.VibrationBlock

data class VibrationCommand(
    val address: String,
    val blocks: List<VibrationBlock>,
    val repeat: Int,
    val sentAt: Long,
)
