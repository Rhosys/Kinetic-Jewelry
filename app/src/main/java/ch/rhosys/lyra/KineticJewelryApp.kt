package ch.rhosys.lyra

import android.app.Application
import android.util.Log
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KineticJewelryApp : Application() {

    /** Startup error captured for display in the UI if initialization fails. */
    var startupError: Throwable? = null
        private set

    override fun onCreate() {
        super.onCreate()
        // Install crash handler FIRST — before anything else can throw
        installCrashHandler()

        try {
            initPostHog()
        } catch (e: Throwable) {
            Log.e("KineticJewelryApp", "PostHog init failed", e)
            startupError = e
            // App continues — PostHog is non-critical
        }
    }

    private fun initPostHog() {
        val config = PostHogAndroidConfig(
            apiKey = "phc_D195RxeDm7isiEPFR31SxBu0KED0Bdc0z9nwSlWM58",
            host = "https://live.rhosys.ch",
        ).apply {
            captureApplicationLifecycleEvents = true
            captureDeepLinks = true
            sessionReplay = true
        }
        PostHogAndroid.setup(this, config)
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                PostHog.capture(
                    event = "app_crashed",
                    properties = mapOf(
                        "exception" to throwable.javaClass.name,
                        "message" to (throwable.message ?: ""),
                        "stacktrace" to throwable.stackTraceToString(),
                    ),
                )
                PostHog.flush()
            } catch (_: Throwable) {
                // PostHog itself may not be initialized — don't double-crash
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
