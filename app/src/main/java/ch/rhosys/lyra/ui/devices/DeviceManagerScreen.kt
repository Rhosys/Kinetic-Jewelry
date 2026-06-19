package ch.rhosys.lyra.ui.devices

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.lyra.domain.model.BluetoothDeviceInfo
import ch.rhosys.lyra.domain.model.ConnectionState
import ch.rhosys.lyra.domain.model.DeviceType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.flow.collectLatest

private val BluetoothDeviceInfo.deviceSubtitle: String
    get() = if (deviceType == DeviceType.WEAR_OS) "Wear OS" else address

private const val MAX_SCAN_NAME_LENGTH = 30

/** Scanned BLE advertised names are untrusted input; strip to a safe display subset. */
private fun sanitizeScanName(rawName: String): String =
    rawName
        .filter { it.isLetterOrDigit() || it == ' ' || it == '-' }
        .take(MAX_SCAN_NAME_LENGTH)

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
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                Text(
                    "Registered Devices",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (alertDevices.isEmpty()) {
                item {
                    Text(
                        "No registered devices yet.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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

            item {
                Text(
                    "Known Devices",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (pairedDevices.isEmpty()) {
                item {
                    Text(
                        "No paired devices found. Pair a device via Bluetooth settings.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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

            item {
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
            }

            if (!blePermissions.allPermissionsGranted) {
                item {
                    Text(
                        "Bluetooth permission is required to scan for nearby devices.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isScanning) {
                item {
                    Column {
                        Text(
                            "Scanning…",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        ScanResultsList(newScanResults)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanResultsList(devices: List<BluetoothDeviceInfo>) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(min = 120.dp, max = 300.dp),
    ) {
        if (devices.isEmpty()) {
            Text(
                "No new devices found yet.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                items(devices, key = { "scan_${it.address}" }) { device ->
                    Text(
                        sanitizeScanName(device.name),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    HorizontalDivider()
                }
            }
        }
    }
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
                val stateLabel =
                    when (device.connectionState) {
                        ConnectionState.CONNECTED -> "Sending…"
                        ConnectionState.CONNECTING -> "Connecting…"
                        ConnectionState.ERROR -> "Error"
                        ConnectionState.DISCONNECTED -> "Idle"
                    }
                SuggestionChip(onClick = {}, label = { Text(stateLabel) })
                Button(onClick = onTest, modifier = Modifier.padding(start = 8.dp)) { Text("Test") }
            }
            OutlinedButton(onClick = onRemove, modifier = Modifier.padding(start = 4.dp)) { Text("Remove") }
        } else {
            Switch(checked = false, onCheckedChange = { if (it) onEnable() })
        }
    }
}
