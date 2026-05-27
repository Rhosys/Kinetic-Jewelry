package com.rhosys.kineticjewelry.data.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.content.Context
import com.rhosys.kineticjewelry.domain.BluetoothController
import com.rhosys.kineticjewelry.domain.VibrationPacketBuilder
import com.rhosys.kineticjewelry.domain.model.BluetoothDeviceInfo
import com.rhosys.kineticjewelry.domain.model.ConnectionState
import com.rhosys.kineticjewelry.domain.model.ProtocolVersion
import com.rhosys.kineticjewelry.domain.model.VibrationMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val SERVICE_UUID       = UUID.fromString("6b2f0001-0000-1000-8000-00805f9b34fb")
private val COMMAND_CHAR_UUID  = UUID.fromString("6b2f0002-0000-1000-8000-00805f9b34fb")
private val FIRMWARE_CHAR_UUID = UUID.fromString("6b2f0004-0000-1000-8000-00805f9b34fb")
private const val CONNECT_TIMEOUT_MS = 15_000L

@Singleton
class BluetoothControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packetBuilder: VibrationPacketBuilder,
) : BluetoothController {

    private val bluetoothAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val connectedDevices: StateFlow<List<BluetoothDeviceInfo>> = _connectedDevices.asStateFlow()

    fun refreshPairedDevices() {
        _pairedDevices.value = bluetoothAdapter.bondedDevices.map { device ->
            BluetoothDeviceInfo(
                address = device.address,
                name = device.name ?: device.address,
                isAlertEnabled = false,
                connectionState = ConnectionState.DISCONNECTED,
            )
        }
    }

    override suspend fun sendVibration(address: String, mode: VibrationMode): Result<Unit> =
        runCatching {
            withTimeout(CONNECT_TIMEOUT_MS) {
                val events = Channel<GattEvent>(capacity = 16)
                val callback = BleGattCallback(events)
                val device = bluetoothAdapter.getRemoteDevice(address)

                @Suppress("MissingPermission")
                val gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)

                try {
                    val connected = events.receive()
                    check(connected is GattEvent.Connected) { "Expected Connected but got $connected" }

                    gatt.discoverServices()
                    val discovered = events.receive()
                    check(discovered is GattEvent.ServicesDiscovered) { "Service discovery failed" }

                    val firmware = readFirmwareVersion(gatt, events)
                    val packets = packetBuilder.buildPackets(mode, firmware)

                    val commandChar = gatt.getService(SERVICE_UUID)
                        ?.getCharacteristic(COMMAND_CHAR_UUID)
                        ?: error("Command characteristic not found on $address")

                    packets.forEachIndexed { index, (bytes, delayMs) ->
                        @Suppress("DEPRECATION", "MissingPermission")
                        commandChar.value = bytes
                        @Suppress("DEPRECATION", "MissingPermission")
                        gatt.writeCharacteristic(commandChar)

                        val writeResult = events.receive()
                        check(writeResult is GattEvent.CharacteristicWritten && writeResult.success) {
                            "Write failed for packet $index on $address"
                        }
                        if (index < packets.lastIndex) delay(delayMs.toLong())
                    }
                } finally {
                    @Suppress("MissingPermission")
                    gatt.disconnect()
                    gatt.close()
                }
            }
        }

    private suspend fun readFirmwareVersion(gatt: BluetoothGatt, events: Channel<GattEvent>): ProtocolVersion {
        val firmwareChar = gatt.getService(SERVICE_UUID)?.getCharacteristic(FIRMWARE_CHAR_UUID)
            ?: return ProtocolVersion.V1

        @Suppress("DEPRECATION", "MissingPermission")
        if (!gatt.readCharacteristic(firmwareChar)) return ProtocolVersion.V1

        return when (val event = events.receive()) {
            is GattEvent.FirmwareVersionRead -> ProtocolVersion.fromInt(event.version)
            else -> ProtocolVersion.V1
        }
    }

    override fun releaseResources() = Unit
}
