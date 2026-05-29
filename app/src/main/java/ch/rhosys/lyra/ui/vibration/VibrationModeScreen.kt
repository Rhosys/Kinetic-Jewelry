package ch.rhosys.lyra.ui.vibration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import ch.rhosys.lyra.domain.model.VibrationMode
import kotlinx.coroutines.flow.collectLatest

@Composable
fun VibrationModeScreen(vm: VibrationModeViewModel = hiltViewModel()) {
    val alertDevices by vm.alertDevices.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.snackbar.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            items(VibrationMode.entries) { mode ->
                ModeCard(
                    mode = mode,
                    onTest = { vm.testMode(mode) },
                )
            }
        }
    }
}

@Composable
private fun ModeCard(mode: VibrationMode, onTest: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(mode.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${mode.blocks.size} blocks · ${mode.totalDurationMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Button(onClick = onTest) { Text("Test") }
        }
    }
}
