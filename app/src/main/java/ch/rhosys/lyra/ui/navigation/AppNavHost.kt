package ch.rhosys.lyra.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ch.rhosys.lyra.ui.apps.AppFilterScreen
import ch.rhosys.lyra.ui.devices.DeviceManagerScreen
import ch.rhosys.lyra.ui.history.NotificationHistoryScreen
import ch.rhosys.lyra.ui.settings.SettingsScreen

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Screen.Apps.route, modifier = modifier) {
        composable(Screen.Apps.route)     { AppFilterScreen() }
        composable(Screen.Devices.route)  { DeviceManagerScreen() }
        composable(Screen.History.route)  { NotificationHistoryScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
}
