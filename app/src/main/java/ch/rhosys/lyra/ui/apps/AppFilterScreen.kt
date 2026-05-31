package ch.rhosys.lyra.ui.apps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
    val contactsByApp by vm.contactsByApp.collectAsState()
    val historyEntries by vm.historyEntries.collectAsState()
    var showUserPicker by remember { mutableStateOf(false) }

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
            Button(
                onClick = { showUserPicker = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add User")
            }
        }

        if (apps.isEmpty()) {
            item {
                Text(
                    "No apps being watched. Tap \"+ Add\" in the History tab to start.",
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(apps, key = { it.packageName }) { app ->
            val contacts = contactsByApp[app.packageName] ?: emptyList()
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                AppSection(
                    app = app,
                    contacts = contacts,
                    onContactLevelChange = { vm.setContactLevelEnabled(app, it) },
                    onContactWatchedChange = { contact, watched -> vm.setContactWatched(contact, watched) },
                    onRemove = { vm.removeApp(app.packageName) },
                )
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
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val appIcon: Drawable? = remember(app.packageName) {
        try { context.packageManager.getApplicationIcon(app.packageName) } catch (_: Exception) { null }
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
                        onClick = { showMenu = false; onRemove() },
                    )
                }
            }
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
                ContactRow(contact = contact, onWatchedChange = { onContactWatchedChange(contact, it) })
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
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
    ) {
        val label = if (contact.groupName.isNotEmpty())
            "${contact.groupName} › ${contact.contactName}"
        else contact.contactName
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = contact.isWatched ?: true, onCheckedChange = onWatchedChange)
    }
}
