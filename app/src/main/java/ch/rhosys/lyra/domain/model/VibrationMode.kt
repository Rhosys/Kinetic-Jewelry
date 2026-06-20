package ch.rhosys.lyra.domain.model

import ch.rhosys.lyra.domain.model.VibrationBlock.CLICK
import ch.rhosys.lyra.domain.model.VibrationBlock.EXTRA_LONG_BUZZ
import ch.rhosys.lyra.domain.model.VibrationBlock.LONG_BUZZ
import ch.rhosys.lyra.domain.model.VibrationBlock.LONG_PAUSE
import ch.rhosys.lyra.domain.model.VibrationBlock.MEDIUM_BUZZ
import ch.rhosys.lyra.domain.model.VibrationBlock.MEDIUM_PAUSE
import ch.rhosys.lyra.domain.model.VibrationBlock.SHORT_BUZZ
import ch.rhosys.lyra.domain.model.VibrationBlock.SHORT_PAUSE

enum class VibrationMode(
    val stableId: Int,
    val displayName: String,
    val blocks: List<VibrationBlock>,
) {
    SHORT_PULSE(1, "Short Pulse", listOf(SHORT_BUZZ)),
    LONG_PULSE(2, "Long Pulse", listOf(LONG_BUZZ)),
    DOUBLE_TAP(3, "Double Tap", listOf(CLICK, SHORT_PAUSE, CLICK)),
    HEARTBEAT(
        4, "Heartbeat",
        listOf(
            // doot doooo, three times
            SHORT_BUZZ, SHORT_PAUSE, MEDIUM_BUZZ, MEDIUM_PAUSE,
            SHORT_BUZZ, SHORT_PAUSE, MEDIUM_BUZZ, MEDIUM_PAUSE,
            SHORT_BUZZ, SHORT_PAUSE, MEDIUM_BUZZ, LONG_PAUSE,
        ),
    ),
    ESCALATING(
        5, "Escalating",
        listOf(
            CLICK, SHORT_PAUSE,
            SHORT_BUZZ, SHORT_PAUSE,
            MEDIUM_BUZZ, SHORT_PAUSE,
            LONG_BUZZ, SHORT_PAUSE,
            EXTRA_LONG_BUZZ,
        ),
    ),
    SOS(
        6, "SOS",
        listOf(
            // S ···
            CLICK, SHORT_PAUSE, CLICK, SHORT_PAUSE, CLICK, MEDIUM_PAUSE,
            // O ———
            SHORT_BUZZ, SHORT_PAUSE, SHORT_BUZZ, SHORT_PAUSE, SHORT_BUZZ, MEDIUM_PAUSE,
            // S ···
            CLICK, SHORT_PAUSE, CLICK, SHORT_PAUSE, CLICK, LONG_PAUSE,
        ),
    );

    val totalDurationMs: Int get() = blocks.sumOf { it.durationMs }

    companion object {
        val default = SHORT_PULSE

        fun fromStableId(id: Int): VibrationMode =
            entries.firstOrNull { it.stableId == id } ?: default
    }
}
