package ch.rhosys.lyra.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.rhosys.lyra.domain.model.NotificationHistoryEntry
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun NotificationHistoryScreen(vm: NotificationHistoryViewModel = hiltViewModel()) {
    val entries by vm.entries.collectAsState()

    if (entries.isEmpty()) {
        Text(
            "No notifications in the last 7 days",
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(entries, key = { it.id }) { entry ->
                HistoryRow(entry)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: NotificationHistoryEntry) {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val timeStr = if (entry.postedAt >= today) {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.postedAt))
    } else {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.postedAt))
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(entry.appLabel, style = MaterialTheme.typography.bodyLarge)
        if (!entry.senderName.isNullOrBlank()) {
            Text(entry.senderName, style = MaterialTheme.typography.bodyMedium)
        }
        Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
