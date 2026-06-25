package ch.rhosys.lyra.ui.devices

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.ConnectionState
import ch.rhosys.lyra.domain.model.DeviceType
import ch.rhosys.lyra.ui.util.verticalScrollbar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.flow.collectLatest

private val BluetoothDeviceInfo.deviceSubtitle: String
    get() = if (deviceType == DeviceType.WEAR_OS) "Wear OS" else sanitizeMacAddress(address)

private const val MAX_SCAN_NAME_LENGTH = 30

/** Caps how much of the screen a single scrollable section may occupy. */
private const val MAX_SECTION_HEIGHT_FRACTION = 0.4f

private val ConnectedColor = Color(0xFF2E7D32)
private val ConnectingColor = Color(0xFFEF6C00)

/** Scanned BLE advertised names are untrusted input; strip to a safe display subset. */
private fun sanitizeScanName(rawName: String): String =
    rawName
        .filter { it.isLetterOrDigit() || it == ' ' || it == '-' }
        .take(MAX_SCAN_NAME_LENGTH)

/** Normalizes a BLE address to the standard uppercase, colon-separated octet form (AA:BB:CC:DD:EE:FF). */
private fun sanitizeMacAddress(rawAddress: String): String =
    rawAddress
        .filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
        .uppercase()
        .chunked(2)
        .joinToString(":")

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DeviceManagerScreen(vm: DeviceManagerViewModel = hiltViewModel()) {
    val blePermissions =
        rememberMultiplePermissionsState(
            buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(Manifest.permission.BLUETOOTH_SCAN)
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
        )

    val alertDevices by vm.alertDevices.collectAsState()
    val pairedDevices by vm.pairedDevices.collectAsState()
    val scanResults by vm.scanResults.collectAsState()
    val isScanning by vm.isScanning.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.snackbar.collectLatest { msg -> snackbarHostState.showSnackbar(msg) }
    }

    LaunchedEffect(Unit) {
        if (blePermissions.allPermissionsGranted) vm.refreshDevices()
    }

    val alertAddresses = alertDevices.map { it.address }.toSet()
    val pairedAddresses = pairedDevices.map { it.address }.toSet()
    val newScanResults =
        remember(scanResults, alertAddresses, pairedAddresses) {
            scanResults.filter { it.address !in alertAddresses && it.address !in pairedAddresses }
        }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.padding(innerPadding)) {
            val sectionMaxHeight = maxHeight * MAX_SECTION_HEIGHT_FRACTION

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Registered Devices",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (alertDevices.isEmpty()) {
                    Text(
                        "No registered devices yet.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    DeviceRowList(maxHeight = sectionMaxHeight) {
                        items(alertDevices, key = { "alert_${it.address}" }) { device ->
                            DeviceRow(
                                device = device,
                                isAlertEnabled = true,
                                onEnable = { vm.enableAlert(device) },
                                onTest = { vm.testDevice(device.address) },
                                onRemove = { vm.disableAlert(device.address) },
                                onReEnable = { vm.reEnableDevice(device.address) },
                            )
                            HorizontalDivider()
                        }
                    }
                }

                Text(
                    "Known Devices",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (pairedDevices.isEmpty()) {
                    Text(
                        "No paired devices found. Pair a device via Bluetooth settings.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    DeviceRowList(maxHeight = sectionMaxHeight) {
                        items(pairedDevices, key = { "paired_${it.address}" }) { device ->
                            DeviceRow(
                                device = device,
                                isAlertEnabled = false,
                                onEnable = { vm.enableAlert(device) },
                                onTest = { vm.testDevice(device.address) },
                                onRemove = { vm.disableAlert(device.address) },
                                onReEnable = { vm.reEnableDevice(device.address) },
                            )
                            HorizontalDivider()
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        when {
                            isScanning -> vm.stopScan()
                            blePermissions.allPermissionsGranted -> vm.startScan()
                            else -> blePermissions.launchMultiplePermissionRequest()
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(64.dp),
                ) {
                    Text(
                        when {
                            isScanning -> "Stop Scanning"
                            !blePermissions.allPermissionsGranted -> "Grant Bluetooth Permission"
                            else -> "Scan for Devices"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                if (!blePermissions.allPermissionsGranted) {
                    Text(
                        "Bluetooth permission is required to scan for nearby devices.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Discovered devices stay visible after scanning stops, until the user leaves this screen.
                if (isScanning || newScanResults.isNotEmpty()) {
                    Text(
                        if (isScanning) "Scanning…" else "Discovered Devices",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    if (isScanning) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    ScanResultsList(newScanResults, maxHeight = sectionMaxHeight)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DeviceRowList(
    maxHeight: Dp,
    content: LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScrollbar(listState),
        content = content,
    )
}

@Composable
private fun ScanResultsList(
    devices: List<BluetoothDeviceInfo>,
    maxHeight: Dp,
) {
    val listState = rememberLazyListState()
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(min = 56.dp, max = maxHeight),
    ) {
        if (devices.isEmpty()) {
            Text(
                "No new devices found yet.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(8.dp).verticalScrollbar(listState),
            ) {
                items(devices, key = { "scan_${it.address}" }) { device ->
                    ScanResultRow(device)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ScanResultRow(device: BluetoothDeviceInfo) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Text(sanitizeScanName(device.name), style = MaterialTheme.typography.bodyLarge)
        Text(sanitizeMacAddress(device.address), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ConnectionStatusChip(state: ConnectionState) {
    val (icon, tint, label) =
        when (state) {
            ConnectionState.CONNECTED -> Triple(Icons.Default.Wifi, ConnectedColor, "Connected")
            ConnectionState.CONNECTING -> Triple(Icons.Default.Wifi, ConnectingColor, "Connecting…")
            ConnectionState.ERROR -> Triple(Icons.Default.Warning, MaterialTheme.colorScheme.error, "Error")
            ConnectionState.DISCONNECTED -> Triple(Icons.Default.Warning, ConnectingColor, "Not Connected")
        }
    Icon(
        icon,
        contentDescription = label,
        tint = tint,
        modifier = Modifier.size(SuggestionChipDefaults.IconSize).padding(horizontal = 4.dp),
    )
}

@Composable
private fun DeviceRow(
    device: BluetoothDeviceInfo,
    isAlertEnabled: Boolean,
    onEnable: () -> Unit,
    onTest: () -> Unit,
    onRemove: () -> Unit,
    onReEnable: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(device.name, style = MaterialTheme.typography.bodyLarge)
            Text(device.deviceSubtitle, style = MaterialTheme.typography.labelSmall)
            if (isAlertEnabled && device.isCurrentlyDisabled) {
                Text(
                    "Auto-disabled — repeated timeouts",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (isAlertEnabled) {
            if (device.isCurrentlyDisabled) {
                Button(onClick = onReEnable, modifier = Modifier.padding(start = 8.dp)) { Text("Re-enable") }
            } else {
                ConnectionStatusChip(device.connectionState)
                Button(onClick = onTest, modifier = Modifier.padding(start = 8.dp)) { Text("Test") }
            }
            OutlinedButton(onClick = onRemove, modifier = Modifier.padding(start = 4.dp)) { Text("Remove") }
        } else {
            Switch(checked = false, onCheckedChange = { if (it) onEnable() })
        }
    }
}
