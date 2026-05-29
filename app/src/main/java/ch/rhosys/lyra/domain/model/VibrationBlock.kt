package ch.rhosys.lyra.domain.model

import ch.rhosys.lyra.domain.model.ProtocolVersion.V1

enum class VibrationBlock(val id: Byte, val durationMs: Int, val since: ProtocolVersion) {
    SHORT_BUZZ  (0x01, 100, V1),
    MEDIUM_BUZZ (0x02, 250, V1),
    LONG_BUZZ   (0x03, 500, V1),
    SHORT_PAUSE (0x04,  80, V1),
    MEDIUM_PAUSE(0x05, 200, V1),
    LONG_PAUSE  (0x06, 600, V1),
    CLICK       (0x07,  40, V1),
}
