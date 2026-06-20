package ch.rhosys.lyra.domain.model

import ch.rhosys.lyra.domain.model.ProtocolVersion.V1

// IDs are 4 bits: 0x1-0x9 are vibrations (motor on), 0xA-0xF are pauses (motor
// off) — packed two-per-byte on the wire, see VibrationPacketBuilder.
enum class VibrationBlock(val id: Byte, val durationMs: Int, val since: ProtocolVersion) {
    CLICK          (0x1, 40,   V1),
    SHORT_BUZZ     (0x2, 100,  V1),
    MEDIUM_BUZZ    (0x3, 250,  V1),
    LONG_BUZZ      (0x4, 500,  V1),
    EXTRA_LONG_BUZZ(0x5, 1000, V1),
    SHORT_PAUSE    (0xA,  80,  V1),
    MEDIUM_PAUSE   (0xB, 200,  V1),
    LONG_PAUSE     (0xC, 600,  V1),
}
