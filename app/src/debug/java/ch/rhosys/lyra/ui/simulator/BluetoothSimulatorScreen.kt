package ch.rhosys.lyra.ui.simulator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.lyra.bluetooth.VibrationCommand
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BluetoothSimulatorScreen(vm: BluetoothSimulatorViewModel = hiltViewModel()) {
    val log by vm.commandLog.collectAsState()
    var packageName by remember { mutableStateOf("com.whatsapp") }
    var contactName by remember { mutableStateOf("Alice") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("BT Simulator", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = packageName,
            onValueChange = { packageName = it },
            label = { Text("Package name") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = contactName,
            onValueChange = { contactName = it },
            label = { Text("Contact name") },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )

        Row(modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = { vm.inject(packageName, contactName) }) { Text("Inject Notification") }
            Button(
                onClick = { vm.clearLog() },
                modifier = Modifier.padding(start = 8.dp),
            ) { Text("Clear Log") }
        }

        Text("Command Log", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))

        LazyColumn {
            items(log.reversed()) { cmd ->
                CommandLogRow(cmd)
            }
        }
    }
}

@Composable
private fun CommandLogRow(cmd: VibrationCommand) {
    val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(cmd.sentAt))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column {
            Text("${cmd.mode.displayName} → ${cmd.address}", style = MaterialTheme.typography.bodyMedium)
            Text(time, style = MaterialTheme.typography.labelSmall)
        }
    }
}
