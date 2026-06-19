package ch.rhosys.lyra

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.rhosys.lyra.domain.AppSettingsProvider
import ch.rhosys.lyra.domain.repository.BluetoothDeviceRepository
import ch.rhosys.lyra.ui.error.StartupErrorScreen
import ch.rhosys.lyra.ui.navigation.AppNavHost
import ch.rhosys.lyra.ui.navigation.Screen
import ch.rhosys.lyra.ui.onboarding.SetupScreen
import ch.rhosys.lyra.ui.onboarding.isNotificationListenerEnabled
import ch.rhosys.lyra.ui.theme.KineticJewelryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var deviceRepo: BluetoothDeviceRepository

    @Inject lateinit var appSettings: AppSettingsProvider

    private var hasNotificationAccess by mutableStateOf(false)
    private var hasBlePermission by mutableStateOf(false)
    private var hasPostNotificationPermission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasNotificationAccess = isNotificationListenerEnabled(this)
        hasBlePermission = checkBlePermission()
        hasPostNotificationPermission = checkPostNotificationPermission()

        // Re-check on every resume so revocations via Settings are reflected immediately
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                hasNotificationAccess = isNotificationListenerEnabled(this@MainActivity)
                hasBlePermission = checkBlePermission()
                hasPostNotificationPermission = checkPostNotificationPermission()
                checkReEnableDevices()
            }
        }

        setContent {
            KineticJewelryTheme {
                // Show startup error if Application.onCreate() caught one
                val startupError = (application as? KineticJewelryApp)?.startupError
                if (startupError != null) {
                    StartupErrorScreen(startupError)
                    return@KineticJewelryTheme
                }

                if (!hasNotificationAccess || !hasBlePermission || !hasPostNotificationPermission) {
                    SetupScreen(notificationAccessGranted = hasNotificationAccess)
                    return@KineticJewelryTheme
                }

                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route

                val tabs =
                    listOf(
                        Triple(Screen.Apps, "Apps", Icons.Default.Notifications),
                        Triple(Screen.Devices, "Devices", Icons.Default.Star),
                        Triple(Screen.History, "History", Icons.Default.Refresh),
                        Triple(Screen.Settings, "Settings", Icons.Default.Settings),
                    )

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            tabs.forEach { (screen, label, icon) ->
                                NavigationBarItem(
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Apps.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(icon, contentDescription = null) },
                                    label = { Text(label) },
                                )
                            }
                        }
                    },
                ) { innerPadding ->
                    AppNavHost(navController, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private suspend fun checkReEnableDevices() {
        if (!appSettings.autoReEnable24h.first()) return
        val now = System.currentTimeMillis()
        deviceRepo
            .getAll()
            .filter { it.isCurrentlyDisabled && now >= (it.disabledUntil ?: 0) + AppSettingsProvider.AUTO_RE_ENABLE_DURATION_MS }
            .forEach { deviceRepo.setDisabledUntil(it.address, null) }
    }

    private fun checkBlePermission(): Boolean {
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        return permissions.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkPostNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
