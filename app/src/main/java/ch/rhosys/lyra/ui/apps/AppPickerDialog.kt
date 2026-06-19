package ch.rhosys.lyra.ui.apps

import android.content.Intent
import android.content.pm.ApplicationInfo
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.rhosys.lyra.data.AppIconCache
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

@Composable
fun AppPickerDialog(
    historyApps: List<Pair<String, String>> = emptyList(),
    onDismiss: () -> Unit,
    onAppSelected: (packageName: String, label: String) -> Unit,
) {
    val context = LocalContext.current

    val installedApps by
        produceState(initialValue = emptyList<InstalledAppInfo>()) {
            value =
                withContext(Dispatchers.IO) {
                    val pm = context.packageManager
                    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    val launcherApps =
                        pm
                            .queryIntentActivities(intent, 0)
                            .filter { ri ->
                                (ri.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                            }.map { ri ->
                                val pkg = ri.activityInfo.packageName
                                val label = ri.loadLabel(pm).toString()
                                val icon =
                                    AppIconCache.loadIcon(context.applicationContext, pkg)
                                        ?: try {
                                            pm.getApplicationIcon(pkg).also { d ->
                                                AppIconCache.saveIcon(context.applicationContext, pkg, d)
                                            }
                                        } catch (_: Exception) { null }
                                InstalledAppInfo(packageName = pkg, label = label, icon = icon)
                            }.distinctBy { it.packageName }

                    val launcherPkgs = launcherApps.map { it.packageName }.toSet()

                    val historyOnly =
                        historyApps
                            .distinctBy { it.first }
                            .filter { (pkg, _) -> pkg !in launcherPkgs }
                            .map { (pkg, label) ->
                                val icon =
                                    AppIconCache.loadIcon(context.applicationContext, pkg)
                                        ?: try {
                                            pm.getApplicationIcon(pkg).also { d ->
                                                AppIconCache.saveIcon(context.applicationContext, pkg, d)
                                            }
                                        } catch (_: Exception) { null }
                                InstalledAppInfo(packageName = pkg, label = label, icon = icon)
                            }

                    (launcherApps + historyOnly).sortedBy { it.label.lowercase() }
                }
        }

    var searchQuery by remember { mutableStateOf("") }
    val filtered =
        remember(searchQuery, installedApps) {
            if (searchQuery.isBlank()) {
                installedApps
            } else {
                installedApps.filter {
                    it.label.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
                }
            }
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
                    "Watch All Notifications",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Select an app to receive vibration alerts for all notifications from it.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered, key = { it.packageName }) { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onAppSelected(app.packageName, app.label) }
                                    .padding(vertical = 12.dp),
                        ) {
                            if (app.icon != null) {
                                Image(
                                    painter = rememberDrawablePainter(app.icon),
                                    contentDescription = app.label,
                                    modifier = Modifier.size(36.dp).clip(CircleShape),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(app.label, style = MaterialTheme.typography.bodyLarge)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
