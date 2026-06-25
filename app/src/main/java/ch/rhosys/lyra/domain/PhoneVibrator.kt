package ch.rhosys.lyra.domain

/**
 * The phone's own vibrator. It implements the same [BluetoothController] interface as BLE/Wear
 * devices, but is injected directly rather than aggregated into the paired/connected device
 * lists — the phone is always available, so it's never scanned for, paired, or enabled/disabled
 * by the user, and every trigger (realtime notification, debug screen, settings demo) calls it
 * explicitly instead of going through the alert-enabled device priority dispatch.
 */
interface PhoneVibrator : BluetoothController {
    companion object {
        const val ADDRESS = "phone"
    }
}
