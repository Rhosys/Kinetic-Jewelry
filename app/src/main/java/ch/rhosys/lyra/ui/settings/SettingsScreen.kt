package ch.rhosys.lyra.ui.settings

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import ch.rhosys.lyra.BuildConfig
import ch.rhosys.lyra.data.LogLevel
import ch.rhosys.lyra.domain.AppSettingsProvider
import ch.rhosys.lyra.domain.model.MultiDeviceMode
import ch.rhosys.lyra.ui.common.ErrorDialog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Entry point for the Settings tab. The Compose compiler disallows try/catch around composable
 * function invocations entirely (not just in catch blocks), so [hiltViewModel] and
 * [SettingsScreenContent] are called directly here. Load-time failures inside non-composable code
 * (DataStore reads, lifecycle effects) are instead surfaced via [SettingsViewModel.loadError] and
 * the [onError] callback below, both backed by plain try/catch in code that isn't itself a
 * composable call.
 */
@Composable
fun SettingsScreen(onDebugVibrationsClick: () -> Unit = {}) {
    var localError by remember { mutableStateOf<Throwable?>(null) }

    val vm: SettingsViewModel = hiltViewModel()
    val vmLoadError by vm.loadError.collectAsState()

    SettingsScreenContent(
        vm = vm,
        onDebugVibrationsClick = onDebugVibrationsClick,
        onError = { e -> localError = e },
    )

    val displayError = localError ?: vmLoadError
    if (displayError != null) {
        ErrorDialog(
            error = displayError,
            onDismiss = {
                localError = null
                vm.clearLoadError()
            },
        )
    }
}

@Composable
private fun SettingsScreenContent(
    vm: SettingsViewModel,
    onDebugVibrationsClick: () -> Unit,
    onError: (Throwable) -> Unit,
) {
    Log.d("SettingsScreen", "SettingsScreen composing")
    val listenerEnabled by vm.listenerEnabled.collectAsState()
    val listenerConnected by vm.listenerConnected.collectAsState()
    val batteryOptimizationIgnored by vm.batteryOptimizationIgnored.collectAsState()
    val logEntries by vm.logEntries.collectAsState()
    val connectionTimeoutMs by vm.connectionTimeoutMs.collectAsState()
    val multiDeviceMode by vm.multiDeviceMode.collectAsState()
    val autoReEnable24h by vm.autoReEnable24h.collectAsState()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        try {
            vm.refreshStatus()
        } catch (e: Throwable) {
            onError(e)
        }
        onPauseOrDispose {}
    }

    val buildDate =
        remember {
            try {
                DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.parse(BuildConfig.BUILD_TIME))
            } catch (e: Throwable) {
                Log.e("SettingsScreen", "Failed to parse BUILD_TIME: '${BuildConfig.BUILD_TIME}'", e)
                onError(e)
                "unknown"
            }
        }

    val scrollState = rememberScrollState()

    Column(
        modifier =
            Modifier
                .verticalScroll(scrollState)
                .padding(16.dp),
    ) {
        Text("Notification Access", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        when {
            !listenerEnabled -> {
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

        if (!batteryOptimizationIgnored) {
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Runtime permissions status
        Text("Permissions", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        var notifPermGranted by remember { mutableStateOf(true) }
        var btPermGranted by remember { mutableStateOf(true) }

        LifecycleResumeEffect(Unit) {
            try {
                notifPermGranted =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                btPermGranted =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
            } catch (e: Throwable) {
                onError(e)
            }
            onPauseOrDispose {}
        }

        PermissionStatusRow("Push Notifications", notifPermGranted)
        PermissionStatusRow("Bluetooth", btPermGranted)

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Connection", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        val timeoutSec = (connectionTimeoutMs / 1_000f)
        Text(
            "Connection timeout: ${timeoutSec.toInt()}s",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = timeoutSec,
            onValueChange = { vm.setConnectionTimeoutMs((it * 1_000).toLong()) },
            valueRange = (AppSettingsProvider.MIN_TIMEOUT_MS / 1_000f)..(AppSettingsProvider.SYSTEM_MAX_TIMEOUT_MS / 1_000f),
            steps = ((AppSettingsProvider.SYSTEM_MAX_TIMEOUT_MS - AppSettingsProvider.MIN_TIMEOUT_MS) / 1_000 - 1).toInt(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("Multi-device mode", style = MaterialTheme.typography.bodyMedium)
        MultiDeviceMode.entries.forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RadioButton(
                    selected = multiDeviceMode == mode,
                    onClick = { vm.setMultiDeviceMode(mode) },
                )
                Column {
                    Text(mode.displayName, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        when (mode) {
                            MultiDeviceMode.ALL_DEVICES -> "Vibrate all devices; wait 1s after first response"
                            MultiDeviceMode.FIRST_WINS -> "Find the best device, and vibrate it"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto re-enable after 24h", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Automatically re-enable devices disabled after repeated timeouts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = autoReEnable24h,
                onCheckedChange = { vm.setAutoReEnable24h(it) },
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Build Info", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodySmall,
        )
        Text("Commit ${BuildConfig.GIT_COMMIT}", style = MaterialTheme.typography.bodySmall)
        Text("Built $buildDate", style = MaterialTheme.typography.bodySmall)

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Debug", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onDebugVibrationsClick) { Text("DEBUG Vibrations") }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Logs", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (logEntries.isNotEmpty()) {
            Row {
                OutlinedButton(onClick = {
                    val text = logEntries.joinToString("\n") { "${it.timestamp} [${it.level}] ${it.message}" }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Kinetic Logs", text))
                    Toast.makeText(context, "Logs copied", Toast.LENGTH_SHORT).show()
                }) { Text("Copy") }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = { vm.clearLogs() }) { Text("Clear") }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 300.dp),
        ) {
            val listState = rememberLazyListState()

            // Auto-scroll to bottom when new entries arrive
            LaunchedEffect(logEntries.size) {
                if (logEntries.isNotEmpty()) {
                    listState.animateScrollToItem(logEntries.lastIndex)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.padding(8.dp),
            ) {
                if (logEntries.isEmpty()) {
                    item {
                        Text(
                            "No logs yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                itemsIndexed(logEntries, key = { index, entry -> "${index}_${entry.timestamp}" }) { _, entry ->
                    val color =
                        when (entry.level) {
                            LogLevel.ERROR -> MaterialTheme.colorScheme.error
                            LogLevel.WARN -> MaterialTheme.colorScheme.tertiary
                            LogLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    Text(
                        text = "${entry.timestamp} ${entry.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = color,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    granted: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Surface(
            color = if (granted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                if (granted) "Granted" else "Not Granted",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
