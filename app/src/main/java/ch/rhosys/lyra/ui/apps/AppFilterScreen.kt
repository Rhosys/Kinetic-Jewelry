package ch.rhosys.lyra.ui.apps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.lyra.domain.model.AppFilter
import ch.rhosys.lyra.domain.model.ContactFilter
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun AppFilterScreen(vm: AppFilterViewModel = hiltViewModel()) {
    val apps by vm.apps.collectAsState()
    val expandedPkg by vm.expandedPackage.collectAsState()
    val contacts by vm.contacts.collectAsState()
    val historyEntries by vm.historyEntries.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    var showUserPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        AppPickerDialog(
            onDismiss = { showPicker = false },
            onAppSelected = { packageName, label ->
                vm.addApp(packageName, label)
                showPicker = false
            },
        )
    }

    if (showUserPicker) {
        UserPickerDialog(
            historyEntries = historyEntries,
            onDismiss = { showUserPicker = false },
            onUserSelected = { senderName, packageName, appLabel ->
                vm.addUser(senderName, packageName, appLabel)
                showUserPicker = false
            },
        )
    }

    LazyColumn {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Button(
                    onClick = { showPicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add App")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showUserPicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add User")
                }
            }
        }

        if (apps.isEmpty()) {
            item {
                Text(
                    "No apps being watched. Add an app or tap \"+ App\" in the History tab.",
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(apps, key = { it.packageName }) { app ->
            AppRow(
                app = app,
                isExpanded = expandedPkg == app.packageName,
                onToggleExpand = { vm.toggleExpanded(app.packageName) },
                onContactLevelChange = { vm.setContactLevelEnabled(app, it) },
                onRemove = { vm.removeApp(app.packageName) },
            )
            if (expandedPkg == app.packageName && app.isContactLevelEnabled) {
                if (contacts.isEmpty()) {
                    Text(
                        "No users yet. They appear here from notification history.",
                        modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                contacts.forEach { contact ->
                    ContactRow(
                        contact = contact,
                        onWatchedChange = { vm.setContactWatched(contact, it) },
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun AppRow(
    app: AppFilter,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onContactLevelChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val appIcon: Drawable? = remember(app.packageName) {
        try { context.packageManager.getApplicationIcon(app.packageName) } catch (_: Exception) { null }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (appIcon != null) {
                Image(
                    painter = rememberDrawablePainter(appIcon),
                    contentDescription = app.appLabel,
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appLabel, style = MaterialTheme.typography.titleMedium)
                val modeLabel = if (app.isContactLevelEnabled) "Selected users" else "All users"
                Text(modeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vibrate for selected users only", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = app.isContactLevelEnabled, onCheckedChange = onContactLevelChange)
            }
            OutlinedButton(onClick = onRemove, modifier = Modifier.padding(top = 4.dp)) {
                Text("Remove from watch list")
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: ContactFilter,
    onWatchedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
    ) {
        val label = if (contact.groupName.isNotEmpty())
            "${contact.groupName} › ${contact.contactName}"
        else contact.contactName
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = contact.isWatched ?: true, onCheckedChange = onWatchedChange)
    }
}
