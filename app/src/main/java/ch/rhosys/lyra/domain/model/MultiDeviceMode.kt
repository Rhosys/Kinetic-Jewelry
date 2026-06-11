package ch.rhosys.lyra.domain.model

enum class MultiDeviceMode(
    val displayName: String,
) {
    ALL_DEVICES("All Devices"),
    FIRST_WINS("First Device Wins"),
}
