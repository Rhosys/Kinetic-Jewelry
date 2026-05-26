package com.rhosys.kineticjewelry.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rhosys.kineticjewelry.ui.apps.AppFilterScreen
import com.rhosys.kineticjewelry.ui.devices.DeviceManagerScreen
import com.rhosys.kineticjewelry.ui.settings.SettingsScreen
import com.rhosys.kineticjewelry.ui.vibration.VibrationModeScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Apps.route,
        modifier = modifier,
    ) {
        composable(Screen.Apps.route)      { AppFilterScreen() }
        composable(Screen.Devices.route)   { DeviceManagerScreen() }
        composable(Screen.Vibration.route) { VibrationModeScreen() }
        composable(Screen.Settings.route)  { SettingsScreen() }
    }
}
