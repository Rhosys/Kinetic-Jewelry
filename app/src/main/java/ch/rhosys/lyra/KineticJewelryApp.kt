package ch.rhosys.lyra

import android.app.Application
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KineticJewelryApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initPostHog()
        installCrashHandler()
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
            PostHog.capture(
                event = "app_crashed",
                properties = mapOf(
                    "exception" to throwable.javaClass.name,
                    "message" to (throwable.message ?: ""),
                    "stacktrace" to throwable.stackTraceToString(),
                ),
            )
            PostHog.flush()
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
