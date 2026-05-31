-keep class ch.rhosys.lyra.data.local.db.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keep @com.google.dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *

# PostHog SDK — uses reflection for serialization
-keep class com.posthog.** { *; }
-dontwarn com.posthog.**

# Accompanist — drawablepainter uses reflection
-keep class com.google.accompanist.** { *; }
-dontwarn com.google.accompanist.**
