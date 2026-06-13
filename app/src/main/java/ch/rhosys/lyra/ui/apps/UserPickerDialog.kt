package ch.rhosys.lyra.ui.apps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import ch.rhosys.lyra.data.AppIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.rhosys.lyra.domain.model.NotificationHistoryEntry
import com.google.accompanist.drawablepainter.rememberDrawablePainter

data class SenderInfo(
    val senderName: String,
    val packageName: String,
    val appLabel: String,
)

@Composable
fun UserPickerDialog(
    historyEntries: List<NotificationHistoryEntry>,
    onDismiss: () -> Unit,
    onUserSelected: (senderName: String, packageName: String, appLabel: String) -> Unit,
    onSwitchToAppPicker: () -> Unit,
) {
    val context = LocalContext.current

    // Deduplicate senders from history
    val senders =
        remember(historyEntries) {
            historyEntries
                .filter { !it.senderName.isNullOrBlank() }
                .map { SenderInfo(it.senderName!!, it.packageName, it.appLabel) }
                .distinctBy { "${it.packageName}:${it.senderName}" }
                .sortedBy { it.senderName.lowercase() }
        }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier =
                Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.8f),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Add User From App",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Select a user from an app that has sent you a message recently. " +
                        "If you don't see a user in this list, ask them to send you a message quickly, " +
                        "and then it will show up here.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))

                val linkText =
                    buildAnnotatedString {
                        append("To select all users for an app, ")
                        pushStringAnnotation(tag = "action", annotation = "app_picker")
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ) {
                            append("watch all notifications for an app")
                        }
                        pop()
                        append(".")
                    }
                ClickableText(
                    text = linkText,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    onClick = { offset ->
                        linkText
                            .getStringAnnotations("action", offset, offset)
                            .firstOrNull()
                            ?.let {
                                onDismiss()
                                onSwitchToAppPicker()
                            }
                    },
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (senders.isEmpty()) {
                    Text(
                        "No messages received yet. Once someone sends you a notification, they'll appear here.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(senders, key = { "${it.packageName}:${it.senderName}" }) { sender ->
                            val appIcon by produceState<Drawable?>(null, sender.packageName) {
                                value = withContext(Dispatchers.IO) {
                                    AppIconCache.loadIcon(context.applicationContext, sender.packageName)
                                        ?: try { context.packageManager.getApplicationIcon(sender.packageName) } catch (_: Exception) { null }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onUserSelected(sender.senderName, sender.packageName, sender.appLabel) }
                                        .padding(vertical = 12.dp),
                            ) {
                                if (appIcon != null) {
                                    Image(
                                        painter = rememberDrawablePainter(appIcon),
                                        contentDescription = sender.appLabel,
                                        modifier = Modifier.size(36.dp).clip(CircleShape),
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column {
                                    Text(sender.senderName, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        sender.appLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
