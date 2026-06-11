package ch.rhosys.lyra.ui.devices

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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

    LaunchedEffect(blePermissions.allPermissionsGranted) {
        if (blePermissions.allPermissionsGranted && !isScanning) {
            vm.refreshDevices()
            vm.startScan()
        }
    }

    LaunchedEffect(Unit) {
        if (blePermissions.allPermissionsGranted) vm.refreshDevices()
    }

    val alertAddresses = alertDevices.map { it.address }.toSet()
    val pairedAddresses = pairedDevices.map { it.address }.toSet()
    val allDevices =
        remember(alertDevices, pairedDevices, scanResults) {
            buildList {
                addAll(alertDevices)
                pairedDevices.forEach { if (it.address !in alertAddresses) add(it) }
                scanResults.forEach {
                    if (it.address !in alertAddresses && it.address !in pairedAddresses) add(it)
                }
            }
        }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
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
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        when {
                            isScanning -> "Stop Scanning"
                            !blePermissions.allPermissionsGranted -> "Grant Bluetooth Permission"
                            else -> "Scan for Devices"
                        },
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
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                }
            }

            if (allDevices.isEmpty() && !isScanning) {
                item {
                    Text(
                        "No devices found. Scan or pair a device via Bluetooth settings.",
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            items(allDevices, key = { it.address }) { device ->
                val isAlert = device.address in alertAddresses
                DeviceRow(
                    device = device,
                    isAlertEnabled = isAlert,
                    onEnable = { vm.enableAlert(device) },
                    onTest = { vm.testDevice(device.address) },
                    onRemove = { vm.disableAlert(device.address) },
                )
                HorizontalDivider()
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
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(device.name, style = MaterialTheme.typography.bodyLarge)
            Text(device.deviceSubtitle, style = MaterialTheme.typography.labelSmall)
        }

        if (isAlertEnabled) {
            val stateLabel =
                when (device.connectionState) {
                    ConnectionState.CONNECTED -> "Sending…"
                    ConnectionState.CONNECTING -> "Connecting…"
                    ConnectionState.ERROR -> "Error"
                    ConnectionState.DISCONNECTED -> "Idle"
                }
            SuggestionChip(onClick = {}, label = { Text(stateLabel) })
            Button(onClick = onTest, modifier = Modifier.padding(start = 8.dp)) { Text("Test") }
            OutlinedButton(onClick = onRemove, modifier = Modifier.padding(start = 4.dp)) { Text("Remove") }
        } else {
            Switch(checked = false, onCheckedChange = { if (it) onEnable() })
        }
    }
}
