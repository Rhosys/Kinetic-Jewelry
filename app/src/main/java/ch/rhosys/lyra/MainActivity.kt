package ch.rhosys.lyra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.rhosys.lyra.ui.navigation.AppNavHost
import ch.rhosys.lyra.ui.navigation.Screen
import ch.rhosys.lyra.ui.theme.KineticJewelryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KineticJewelryTheme {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route

                val tabs = listOf(
                    Triple(Screen.Apps,      "Apps",      Icons.Default.Notifications),
                    Triple(Screen.Devices,   "Devices",   Icons.Default.Star),
                    Triple(Screen.Vibration, "Vibration", Icons.Default.Vibration),
                    Triple(Screen.Settings,  "Settings",  Icons.Default.Settings),
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
