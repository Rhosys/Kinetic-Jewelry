package ch.rhosys.lyra.domain.model

data class BluetoothDeviceInfo(
    val address: String,
    val name: String,
    val isAlertEnabled: Boolean,
    val connectionState: ConnectionState,
    val deviceType: DeviceType = DeviceType.BLE_JEWELRY,
    /** Whether the user has added this device to their Favorites. Independent of [isAlertEnabled] — a
     * disabled favorite still shows in the Favorites list; a non-favorite is a Recent Device. */
    val isFavorite: Boolean = false,
    /** Per-device timeout ceiling in ms. null = use the global user setting. */
    val connectionTimeoutMs: Long? = null,
    /** Epoch ms until which this device is suppressed after repeated timeouts. null = active. */
    val disabledUntil: Long? = null,
    val consecutiveTimeouts: Int = 0,
    /** Epoch ms of last successful vibration delivery. null = never succeeded. */
    val lastSuccessAt: Long? = null,
) {
    val isCurrentlyDisabled: Boolean
        get() = disabledUntil != null && System.currentTimeMillis() < disabledUntil
}
