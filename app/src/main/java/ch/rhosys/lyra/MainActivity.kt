package ch.rhosys.lyra

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.rhosys.lyra.ui.navigation.AppNavHost
import ch.rhosys.lyra.ui.navigation.Screen
import ch.rhosys.lyra.ui.onboarding.SetupScreen
import ch.rhosys.lyra.ui.onboarding.isNotificationListenerEnabled
import ch.rhosys.lyra.ui.theme.KineticJewelryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var hasNotificationAccess by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasNotificationAccess = isNotificationListenerEnabled(this)

        // Re-check when returning from Settings
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                hasNotificationAccess = isNotificationListenerEnabled(this@MainActivity)
            }
        }

        setContent {
            KineticJewelryTheme {
                if (!hasNotificationAccess) {
                    SetupScreen(notificationAccessGranted = false)
                    return@KineticJewelryTheme
                }

                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route

                val tabs = listOf(
                    Triple(Screen.Apps,     "Apps",     Icons.Default.Notifications),
                    Triple(Screen.Devices,  "Devices",  Icons.Default.Star),
                    Triple(Screen.History,  "History",  Icons.Default.Refresh),
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
                    }
                ) { innerPadding ->
                    AppNavHost(navController, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
