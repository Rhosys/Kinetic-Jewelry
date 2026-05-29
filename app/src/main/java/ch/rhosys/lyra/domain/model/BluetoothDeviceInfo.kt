package ch.rhosys.lyra.domain.model

data class BluetoothDeviceInfo(
    val address: String,
    val name: String,
    val isAlertEnabled: Boolean,
    val connectionState: ConnectionState,
)
