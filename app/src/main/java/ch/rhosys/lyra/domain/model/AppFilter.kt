package ch.rhosys.lyra.domain.model

data class AppFilter(
    val packageName: String,
    val appLabel: String,
    val isWatched: Boolean,
    val vibrationMode: VibrationMode,
    val isContactLevelEnabled: Boolean,
    // Set once this app has posted at least one CATEGORY_CALL notification, so the UI can
    // offer a call-specific vibration pattern before the user has ever actually received a call.
    val hasCallCategory: Boolean = false,
    // Null means "use vibrationMode for calls too" — mirrors how a null contact-level mode
    // means "use the app default".
    val callVibrationMode: VibrationMode? = null,
)
