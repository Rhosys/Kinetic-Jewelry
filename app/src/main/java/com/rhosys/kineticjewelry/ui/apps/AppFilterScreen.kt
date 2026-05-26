package com.rhosys.kineticjewelry.ui.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import com.rhosys.kineticjewelry.domain.model.AppFilter
import com.rhosys.kineticjewelry.domain.model.ContactFilter
import com.rhosys.kineticjewelry.domain.model.VibrationMode

@Composable
fun AppFilterScreen(vm: AppFilterViewModel = hiltViewModel()) {
    val apps by vm.apps.collectAsState()
    val expandedPkg by vm.expandedPackage.collectAsState()
    val contacts by vm.contacts.collectAsState()

    LazyColumn {
        items(apps, key = { it.packageName }) { app ->
            AppRow(
                app = app,
                isExpanded = expandedPkg == app.packageName,
                onToggleExpand = { vm.toggleExpanded(app.packageName) },
                onWatchedChange = { vm.setAppWatched(app, it) },
                onModeChange = { vm.setAppVibrationMode(app, it) },
            )
            if (expandedPkg == app.packageName) {
                contacts.forEach { contact ->
                    ContactRow(
                        contact = contact,
                        appMode = app.vibrationMode,
                        onWatchedChange = { vm.setContactWatched(contact, it) },
                        onModeChange = { vm.setContactVibrationMode(contact, it) },
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRow(
    app: AppFilter,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onWatchedChange: (Boolean) -> Unit,
    onModeChange: (VibrationMode) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(app.appLabel, style = MaterialTheme.typography.titleMedium)
            Text(app.packageName, style = MaterialTheme.typography.labelSmall)
        }
        VibrationModeDropdown(selected = app.vibrationMode, onSelected = onModeChange)
        Switch(checked = app.isWatched, onCheckedChange = onWatchedChange)
    }
}

@Composable
private fun ContactRow(
    contact: ContactFilter,
    appMode: VibrationMode,
    onWatchedChange: (Boolean?) -> Unit,
    onModeChange: (VibrationMode?) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val label = if (contact.groupName.isNotEmpty())
                "${contact.groupName} › ${contact.contactName}"
            else contact.contactName
            Text(label, style = MaterialTheme.typography.bodyMedium)
            val modeLabel = contact.vibrationMode?.displayName ?: "(inherited: ${appMode.displayName})"
            Text(modeLabel, style = MaterialTheme.typography.labelSmall)
        }
        VibrationModeDropdown(
            selected = contact.vibrationMode ?: appMode,
            onSelected = { onModeChange(it) },
        )
        Switch(
            checked = contact.isWatched ?: true,
            onCheckedChange = { onWatchedChange(it) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrationModeDropdown(
    selected: VibrationMode,
    onSelected: (VibrationMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            VibrationMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.displayName) },
                    onClick = { onSelected(mode); expanded = false },
                )
            }
        }
    }
}
