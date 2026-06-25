package ch.rhosys.lyra.fake

import ch.rhosys.lyra.domain.PhoneVibrator
import ch.rhosys.lyra.domain.model.VibrationBlock

class FakePhoneVibrator : PhoneVibrator {
    val sentCommands = mutableListOf<VibrationCommand>()

    override suspend fun sendVibration(
        blocks: List<VibrationBlock>,
        repeat: Int,
    ): Result<Unit> {
        sentCommands += VibrationCommand("phone", blocks, repeat, System.currentTimeMillis())
        return Result.success(Unit)
    }
}
