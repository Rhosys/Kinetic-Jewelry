package com.rhosys.kineticjewelry.domain.model

enum class VibrationMode(val displayName: String, val pattern: List<Pair<Int, Int>>) {
    SHORT_PULSE(
        "Short Pulse",
        listOf(100 to 100)
    ),
    LONG_PULSE(
        "Long Pulse",
        listOf(500 to 200)
    ),
    DOUBLE_TAP(
        "Double Tap",
        listOf(100 to 100, 100 to 200)
    ),
    HEARTBEAT(
        "Heartbeat",
        listOf(80 to 60, 200 to 400)
    ),
    SOS(
        "SOS",
        listOf(
            100 to 100, 100 to 100, 100 to 300,  // · · ·
            300 to 100, 300 to 100, 300 to 300,  // — — —
            100 to 100, 100 to 100, 100 to 700   // · · ·
        )
    ),
    ESCALATING(
        "Escalating",
        listOf(50 to 200, 100 to 200, 200 to 200, 400 to 200)
    );

    companion object {
        val default = SHORT_PULSE
    }
}
