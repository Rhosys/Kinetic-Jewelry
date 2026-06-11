package ch.rhosys.lyra.domain.model

data class BluetoothDeviceInfo(
    val address: String,
    val name: String,
    val isAlertEnabled: Boolean,
    val connectionState: ConnectionState,
    val deviceType: DeviceType = DeviceType.BLE_JEWELRY,
    /** Per-device timeout ceiling in ms. null = use the global user setting. */
    val connectionTimeoutMs: Long? = null,
    /** Epoch ms until which this device is suppressed after repeated timeouts. null = active. */
    val disabledUntil: Long? = null,
    val consecutiveTimeouts: Int = 0,
) {
    val isCurrentlyDisabled: Boolean
        get() = disabledUntil != null && System.currentTimeMillis() < disabledUntil
}
