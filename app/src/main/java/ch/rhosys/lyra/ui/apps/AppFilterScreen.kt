package ch.rhosys.lyra.ui.apps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.lyra.domain.model.AppFilter
import ch.rhosys.lyra.domain.model.ContactFilter
import ch.rhosys.lyra.domain.model.VibrationMode
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun AppFilterScreen(vm: AppFilterViewModel = hiltViewModel()) {
    val apps by vm.apps.collectAsState()
    val contactsByApp by vm.contactsByApp.collectAsState()
    val historyEntries by vm.historyEntries.collectAsState()
    var showUserPicker by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }

    if (showUserPicker) {
        UserPickerDialog(
            historyEntries = historyEntries,
            onDismiss = { showUserPicker = false },
            onUserSelected = { senderName, packageName, appLabel ->
                vm.addUser(senderName, packageName, appLabel)
                showUserPicker = false
            },
            onSwitchToAppPicker = { showAppPicker = true },
        )
    }

    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = { packageName, label ->
                vm.addApp(packageName, label)
                showAppPicker = false
            },
        )
    }

    val context = LocalContext.current
    val appName = remember { context.applicationInfo.loadLabel(context.packageManager).toString() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = appName,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
        )

        Text(
            text = "Current Watched Notifiers",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (apps.isEmpty()) {
                item {
                    Text(
                        "No apps being watched. Tap Add below to start.",
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(apps, key = { it.packageName }) { app ->
                val contacts = contactsByApp[app.packageName] ?: emptyList()
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    AppSection(
                        app = app,
                        contacts = contacts,
                        onContactLevelChange = { vm.setContactLevelEnabled(app, it) },
                        onContactWatchedChange = { contact, watched -> vm.setContactWatched(contact, watched) },
                        onVibrationModeChange = { vm.setAppVibrationMode(app.packageName, it) },
                        onContactVibrationModeChange = { contact, mode -> vm.setContactVibrationMode(contact, mode) },
                        onRemove = { vm.removeApp(app.packageName) },
                    )
                }
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { showUserPicker = true },
                modifier =
                    Modifier
                        .weight(1f)
                        .height(88.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Add User")
                }
            }
            Button(
                onClick = { showAppPicker = true },
                modifier =
                    Modifier
                        .weight(1f)
                        .height(88.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Add App")
                }
            }
        }
    }
}

@Composable
private fun AppSection(
    app: AppFilter,
    contacts: List<ContactFilter>,
    onContactLevelChange: (Boolean) -> Unit,
    onContactWatchedChange: (ContactFilter, Boolean) -> Unit,
    onVibrationModeChange: (VibrationMode) -> Unit,
    onContactVibrationModeChange: (ContactFilter, VibrationMode?) -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val appIcon: Drawable? =
        remember(app.packageName) {
            try {
                context.packageManager.getApplicationIcon(app.packageName)
            } catch (_: Exception) {
                null
            }
        }
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // App header row
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (appIcon != null) {
                Image(
                    painter = rememberDrawablePainter(appIcon),
                    contentDescription = app.appLabel,
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(app.appLabel, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Remove App") },
                        onClick = {
                            showMenu = false
                            onRemove()
                        },
                    )
                }
            }
        }

        // Vibration mode (app-level)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text("Vibration", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            VibrationModePicker(mode = app.vibrationMode, onModeSelected = onVibrationModeChange)
        }

        // "All Users" toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text("All Users", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = !app.isContactLevelEnabled, onCheckedChange = { onContactLevelChange(!it) })
        }

        // User list (always visible when contact-level is enabled)
        if (app.isContactLevelEnabled) {
            if (contacts.isEmpty()) {
                Text(
                    "No users yet. They appear from notification history.",
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            contacts.forEach { contact ->
                ContactRow(
                    contact = contact,
                    onWatchedChange = { onContactWatchedChange(contact, it) },
                    onVibrationModeChange = { onContactVibrationModeChange(contact, it) },
                )
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: ContactFilter,
    onWatchedChange: (Boolean) -> Unit,
    onVibrationModeChange: (VibrationMode?) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
    ) {
        val label =
            if (contact.groupName.isNotEmpty()) {
                "${contact.groupName} › ${contact.contactName}"
            } else {
                contact.contactName
            }
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        NullableVibrationModePicker(mode = contact.vibrationMode, onModeSelected = onVibrationModeChange)
        Switch(checked = contact.isWatched ?: true, onCheckedChange = onWatchedChange)
    }
}

@Composable
private fun VibrationModePicker(
    mode: VibrationMode,
    onModeSelected: (VibrationMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text(mode.displayName) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            VibrationMode.entries.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m.displayName) },
                    onClick = {
                        expanded = false
                        onModeSelected(m)
                    },
                )
            }
        }
    }
}

@Composable
private fun NullableVibrationModePicker(
    mode: VibrationMode?,
    onModeSelected: (VibrationMode?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text(mode?.displayName ?: "App default") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("App default") },
                onClick = {
                    expanded = false
                    onModeSelected(null)
                },
            )
            VibrationMode.entries.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m.displayName) },
                    onClick = {
                        expanded = false
                        onModeSelected(m)
                    },
                )
            }
        }
    }
}
