package ch.rhosys.lyra.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val listenerConnected by vm.listenerConnected.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Notification Access", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        when {
            !vm.listenerEnabled -> {
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        "Not Granted",
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Button(
                    onClick = { vm.openNotificationListenerSettings(context) },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Grant Access") }
            }
            !listenerConnected -> {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        "Enabled (rebinding…)",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Button(
                    onClick = { vm.requestRebind() },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Reconnect Service") }
            }
            else -> {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        "Connected",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
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
