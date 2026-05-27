package com.rhosys.kineticjewelry.ui.devices

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rhosys.kineticjewelry.domain.model.BluetoothDeviceInfo
import com.rhosys.kineticjewelry.domain.model.ConnectionState

@Composable
fun DeviceManagerScreen(vm: DeviceManagerViewModel = hiltViewModel()) {
    val alertDevices by vm.alertDevices.collectAsState()
    val pairedDevices by vm.pairedDevices.collectAsState()

    LazyColumn {
        if (alertDevices.isNotEmpty()) {
            item { SectionHeader("Alert-Enabled Devices") }
            items(alertDevices, key = { it.address }) { device ->
                AlertDeviceRow(
                    device = device,
                    onTest = { vm.testDevice(device.address) },
                    onRemove = { vm.disableAlert(device.address) },
                )
                HorizontalDivider()
            }
        }
        if (pairedDevices.isNotEmpty()) {
            item { SectionHeader("Other Paired Devices") }
            items(pairedDevices, key = { it.address }) { device ->
                PairedDeviceRow(device = device, onEnable = { vm.enableAlert(device) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(16.dp),
    )
}

@Composable
private fun AlertDeviceRow(
    device: BluetoothDeviceInfo,
    onTest: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(device.name, style = MaterialTheme.typography.bodyLarge)
            Text(device.address, style = MaterialTheme.typography.labelSmall)
        }
        val stateLabel = when (device.connectionState) {
            ConnectionState.CONNECTED    -> "Sending…"
            ConnectionState.CONNECTING   -> "Connecting…"
            ConnectionState.ERROR        -> "Error"
            ConnectionState.DISCONNECTED -> "Idle"
        }
        SuggestionChip(onClick = {}, label = { Text(stateLabel) })
        Button(onClick = onTest, modifier = Modifier.padding(start = 8.dp)) { Text("Test") }
        OutlinedButton(onClick = onRemove, modifier = Modifier.padding(start = 4.dp)) { Text("Remove") }
    }
}

@Composable
private fun PairedDeviceRow(
    device: BluetoothDeviceInfo,
    onEnable: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(device.name, style = MaterialTheme.typography.bodyLarge)
            Text(device.address, style = MaterialTheme.typography.labelSmall)
        }
        Switch(checked = false, onCheckedChange = { if (it) onEnable() })
    }
}
