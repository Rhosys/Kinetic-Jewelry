package ch.rhosys.lyra.ui.apps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ch.rhosys.lyra.domain.model.NotificationHistoryEntry
import com.google.accompanist.drawablepainter.rememberDrawablePainter

data class SenderInfo(
    val senderName: String,
    val packageName: String,
    val appLabel: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPickerDialog(
    historyEntries: List<NotificationHistoryEntry>,
    onDismiss: () -> Unit,
    onUserSelected: (senderName: String, packageName: String, appLabel: String) -> Unit,
) {
    val context = LocalContext.current

    // Deduplicate senders
    val allSenders = remember(historyEntries) {
        historyEntries
            .filter { !it.senderName.isNullOrBlank() }
            .map { SenderInfo(it.senderName!!, it.packageName, it.appLabel) }
            .distinctBy { "${it.packageName}:${it.senderName}" }
            .sortedBy { it.senderName.lowercase() }
    }

    // Distinct apps for filter
    val appLabels = remember(allSenders) {
        listOf("All apps") + allSenders.map { it.appLabel }.distinct().sorted()
    }

    var selectedAppFilter by remember { mutableStateOf("All apps") }
    var appFilterExpanded by remember { mutableStateOf(false) }

    val filtered = remember(selectedAppFilter, allSenders) {
        if (selectedAppFilter == "All apps") allSenders
        else allSenders.filter { it.appLabel == selectedAppFilter }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add User to Watch List", style = MaterialTheme.typography.titleMedium)

                // App filter dropdown
                ExposedDropdownMenuBox(
                    expanded = appFilterExpanded,
                    onExpandedChange = { appFilterExpanded = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    TextField(
                        value = selectedAppFilter,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter by app") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(appFilterExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = appFilterExpanded,
                        onDismissRequest = { appFilterExpanded = false },
                    ) {
                        appLabels.forEach { label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedAppFilter = label
                                    appFilterExpanded = false
                                },
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    Text(
                        "No users found. Users appear here after notifications arrive.",
                        modifier = Modifier.padding(vertical = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                LazyColumn {
                    items(filtered, key = { "${it.packageName}:${it.senderName}" }) { sender ->
                        val appIcon: Drawable? = remember(sender.packageName) {
                            try { context.packageManager.getApplicationIcon(sender.packageName) }
                            catch (_: Exception) { null }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUserSelected(sender.senderName, sender.packageName, sender.appLabel) }
                                .padding(vertical = 8.dp),
                        ) {
                            if (appIcon != null) {
                                Image(
                                    painter = rememberDrawablePainter(appIcon),
                                    contentDescription = sender.appLabel,
                                    modifier = Modifier.size(28.dp).clip(CircleShape),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Column {
                                Text(sender.senderName, style = MaterialTheme.typography.bodyMedium)
                                Text(sender.appLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
