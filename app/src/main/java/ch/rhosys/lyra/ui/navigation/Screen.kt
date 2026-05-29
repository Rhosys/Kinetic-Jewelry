package ch.rhosys.lyra.ui.navigation

sealed class Screen(val route: String) {
    object Apps       : Screen("apps")
    object Devices    : Screen("devices")
    object Vibration  : Screen("vibration")
    object Settings   : Screen("settings")
}
