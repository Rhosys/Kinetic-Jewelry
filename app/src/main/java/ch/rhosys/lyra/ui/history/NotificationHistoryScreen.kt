package ch.rhosys.lyra.ui.history

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.lyra.domain.model.NotificationHistoryEntry
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun NotificationHistoryScreen(vm: NotificationHistoryViewModel = hiltViewModel()) {
    val entries by vm.entries.collectAsState()

    // Deduplicate: keep only the latest entry per (packageName, senderName)
    val deduped = remember(entries) {
        entries
            .groupBy { "${it.packageName}:${it.senderName ?: ""}" }
            .values
            .mapNotNull { group -> group.maxByOrNull { it.postedAt } }
            .sortedByDescending { it.postedAt }
    }

    if (deduped.isEmpty()) {
        Text(
            "No notifications in the last 7 days",
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(deduped, key = { "${it.packageName}:${it.senderName}" }) { entry ->
                HistoryRow(
                    entry = entry,
                    onAdd = { vm.watchUser(entry) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: NotificationHistoryEntry,
    onAdd: () -> Unit,
) {
    val context = LocalContext.current
    val appIcon: Drawable? = remember(entry.packageName) {
        try {
            context.packageManager.getApplicationIcon(entry.packageName)
        } catch (_: Exception) { null }
    }

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val timeStr = if (entry.postedAt >= today) {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.postedAt))
    } else {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.postedAt))
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // App icon
        if (appIcon != null) {
            Image(
                painter = rememberDrawablePainter(appIcon),
                contentDescription = entry.appLabel,
                modifier = Modifier.size(40.dp).clip(CircleShape),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(entry.appLabel, style = MaterialTheme.typography.bodyLarge)
            if (!entry.senderName.isNullOrBlank()) {
                Text(entry.senderName, style = MaterialTheme.typography.bodyMedium)
            }
            Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        OutlinedButton(onClick = onAdd) {
            Text("+ Add", style = MaterialTheme.typography.labelSmall)
        }
    }
}
