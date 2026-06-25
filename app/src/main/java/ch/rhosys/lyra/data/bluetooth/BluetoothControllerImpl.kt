package ch.rhosys.lyra.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import ch.rhosys.lyra.data.AppLogger
import ch.rhosys.lyra.domain.BluetoothController
import ch.rhosys.lyra.domain.VibrationPacketBuilder
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.ConnectionState
import ch.rhosys.lyra.domain.model.ProtocolVersion
import ch.rhosys.lyra.domain.model.VibrationBlock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

private const val SCAN_TIMEOUT_MS = 10_000L

@SuppressLint("MissingPermission") // Permissions checked at UI layer before invoking controller
@Singleton
class BluetoothControllerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val packetBuilder: VibrationPacketBuilder,
        private val logger: AppLogger,
    ) : BluetoothController {
        private val bluetoothAdapter =
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        private val protocolIds = BleProtocolIds.load(context)

        private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
        override val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

        private val _connectedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
        override val connectedDevices: StateFlow<List<BluetoothDeviceInfo>> = _connectedDevices.asStateFlow()

        private val _scanResults = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
        override val scanResults: StateFlow<List<BluetoothDeviceInfo>> = _scanResults.asStateFlow()

        private val _isScanning = MutableStateFlow(false)
        override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

        private val scope = CoroutineScope(Dispatchers.Main.immediate)
        private var scanTimeoutJob: Job? = null

        private val scanCallback =
            object : ScanCallback() {
                override fun onScanResult(
                    callbackType: Int,
                    result: ScanResult,
                ) {
                    val device = result.device
                    val info =
                        BluetoothDeviceInfo(
                            address = device.address,
                            name = device.name ?: device.address,
                            isAlertEnabled = false,
                            connectionState = ConnectionState.DISCONNECTED,
                        )
                    val current = _scanResults.value
                    if (current.none { it.address == info.address }) {
                        _scanResults.value = current + info
                    }
                }
            }

        override fun startScan() {
            _scanResults.value = emptyList()
            _isScanning.value = true
            logger.info("BLE scan started")
            try {
                bluetoothAdapter.bluetoothLeScanner?.startScan(scanCallback)
                    ?: logger.error("BLE scanner unavailable — is Bluetooth enabled?")
            } catch (e: SecurityException) {
                logger.error("BLE scan permission denied", e)
                _isScanning.value = false
                return
            }
            scanTimeoutJob?.cancel()
            scanTimeoutJob =
                scope.launch {
                    delay(SCAN_TIMEOUT_MS)
                    stopScan()
                }
        }

        override fun stopScan() {
            scanTimeoutJob?.cancel()
            scanTimeoutJob = null
            try {
                bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
            } catch (_: SecurityException) {
                // Permission revoked — scanner already inactive
            }
            _isScanning.value = false
            logger.info("BLE scan stopped (${_scanResults.value.size} device(s) found)")
        }

        override fun refreshPairedDevices() {
            try {
                _pairedDevices.value =
                    bluetoothAdapter.bondedDevices.map { device ->
                        BluetoothDeviceInfo(
                            address = device.address,
                            name = device.name ?: device.address,
                            isAlertEnabled = false,
                            connectionState = ConnectionState.DISCONNECTED,
                        )
                    }
            } catch (_: SecurityException) {
                // Permission revoked at runtime — leave existing list intact
            }
        }

        override suspend fun sendVibration(
            address: String,
            blocks: List<VibrationBlock>,
            repeat: Int,
            timeoutMs: Long,
        ): Result<Unit> {
            logger.info("Sending vibration ($blocks × $repeat) to $address")
            return writeBlocks(address, blocks, repeat, timeoutMs)
                .also { result ->
                    result.onSuccess { logger.info("Vibration sent to $address") }
                    result.onFailure { logger.error("Vibration failed for $address", it) }
                }
        }

        private suspend fun writeBlocks(
            address: String,
            blocks: List<VibrationBlock>,
            repeat: Int,
            timeoutMs: Long,
        ): Result<Unit> =
            runCatching {
                withTimeout(timeoutMs) {
                    val events = Channel<GattEvent>(capacity = 16)
                    val callback = BleGattCallback(events, protocolIds.firmwareCharUuid)
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
                        val packets = packetBuilder.buildPackets(blocks, firmware, repeat)

                        val commandChar =
                            gatt
                                .getService(protocolIds.serviceUuid)
                                ?.getCharacteristic(protocolIds.commandCharUuid)
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

        private suspend fun readFirmwareVersion(
            gatt: BluetoothGatt,
            events: Channel<GattEvent>,
        ): ProtocolVersion {
            val firmwareChar =
                gatt.getService(protocolIds.serviceUuid)?.getCharacteristic(protocolIds.firmwareCharUuid)
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
