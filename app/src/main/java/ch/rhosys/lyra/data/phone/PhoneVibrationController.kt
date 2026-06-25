package ch.rhosys.lyra.data.phone

import android.content.Context
import ch.rhosys.lyra.domain.PhoneVibrator
import ch.rhosys.lyra.domain.model.VibrationBlock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneVibrationController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PhoneVibrator {
        override suspend fun sendVibration(
            blocks: List<VibrationBlock>,
            repeat: Int,
        ): Result<Unit> = runCatching { previewVibration(context, blocks, repeat) }
    }
