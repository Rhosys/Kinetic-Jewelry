package com.rhosys.kineticjewelry.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val listenerConnected by vm.listenerConnected.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Notification Listener", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Status", modifier = Modifier.weight(1f))
            val label = when {
                listenerConnected        -> "Connected"
                vm.listenerEnabled      -> "Enabled (rebinding…)"
                else                    -> "Not granted"
            }
            SuggestionChip(onClick = {}, label = { Text(label) })
        }

        if (!vm.listenerEnabled) {
            Button(
                onClick = { vm.openNotificationListenerSettings(context) },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Grant Access") }
        } else if (!listenerConnected) {
            Button(
                onClick = { vm.requestRebind() },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Reconnect Service") }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Battery Optimization", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (!vm.batteryOptimizationIgnored) {
            Text(
                "Battery optimization may kill the service on some devices (Xiaomi, Huawei, OnePlus).",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = { vm.openBatteryOptimizationSettings(context) },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Disable Optimization") }
        } else {
            Text("Battery optimization is disabled for this app.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
