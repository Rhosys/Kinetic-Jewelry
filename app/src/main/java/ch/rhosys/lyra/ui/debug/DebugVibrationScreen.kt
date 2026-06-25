package ch.rhosys.lyra.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.lyra.domain.VibrationPacketBuilder
import ch.rhosys.lyra.domain.model.VibrationBlock
import ch.rhosys.lyra.domain.model.VibrationMode
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DebugVibrationScreen(vm: DebugVibrationViewModel = hiltViewModel()) {
    val alertDevices by vm.alertDevices.collectAsState()
    val sequence by vm.sequence.collectAsState()
    val repeat by vm.repeat.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.snackbar.collectLatest { msg -> snackbarHostState.showSnackbar(msg) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            Text("Connected devices: ${alertDevices.size}", style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text("Load from existing mode", style = MaterialTheme.typography.titleMedium)
            ModeDropdown(onModeSelected = vm::applyMode)

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text("Add a block to the sequence", style = MaterialTheme.typography.titleMedium)
            VibrationBlock.entries.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { block ->
                        OutlinedButton(onClick = { vm.addBlock(block) }, modifier = Modifier.weight(1f)) {
                            Text("${block.name} (${block.durationMs}ms)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Sequence", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = vm::clearSequence) { Text("Clear") }
            }

            if (sequence.isEmpty()) {
                Text(
                    "No blocks yet — tap a block above to add it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    sequence.forEachIndexed { index, block ->
                        AssistChip(
                            onClick = { vm.removeBlockAt(index) },
                            label = { Text(block.name) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove") },
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text("Repeat: $repeat", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = { vm.setRepeat(repeat - 1) }) { Text("-") }
                OutlinedButton(onClick = { vm.setRepeat(repeat + 1) }) { Text("+") }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text("Raw bytes", style = MaterialTheme.typography.titleMedium)
            val hex =
                if (sequence.isEmpty()) {
                    "—"
                } else {
                    VibrationPacketBuilder
                        .encodePacket(sequence.map { it.id.toInt() }, firmwareVersion = 1, repeat = repeat)
                        .joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
                }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            ) {
                Text(hex, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Button(
                onClick = vm::vibrate,
                enabled = sequence.size >= 3,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Vibrate")
            }
        }
    }
}

@Composable
private fun ModeDropdown(onModeSelected: (VibrationMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("Select mode…") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            VibrationMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.displayName) },
                    onClick = {
                        expanded = false
                        onModeSelected(mode)
                    },
                )
            }
        }
    }
}
