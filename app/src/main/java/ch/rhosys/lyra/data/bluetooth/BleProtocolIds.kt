package ch.rhosys.lyra.data.bluetooth

import android.content.Context
import org.json.JSONObject
import java.util.UUID

// BLE GATT UUIDs, loaded at runtime from the repo-root ble-protocol.json
// (packaged into app assets by app/build.gradle.kts). The firmware embeds the
// same file at compile time via firmware/protocol/build.rs, so there is one
// canonical copy of these values instead of hand-synced literals on each side.
data class BleProtocolIds(
    val serviceUuid: UUID,
    val commandCharUuid: UUID,
    val firmwareCharUuid: UUID,
) {
    companion object {
        private const val ASSET_NAME = "ble-protocol.json"

        fun load(context: Context): BleProtocolIds {
            val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            val obj = JSONObject(json)
            return BleProtocolIds(
                serviceUuid = UUID.fromString(obj.getString("service_uuid")),
                commandCharUuid = UUID.fromString(obj.getString("command_characteristic_uuid")),
                firmwareCharUuid = UUID.fromString(obj.getString("firmware_characteristic_uuid")),
            )
        }
    }
}
