# TODO

## Port Robolectric startup test pattern to other Android / Kotlin apps

The pattern introduced in `app/src/test/java/ch/rhosys/lyra/MainActivityStartupTest.kt`
catches startup crashes (e.g. `CompositionLocal not present`, Hilt injection failures,
uncaught exceptions in `Application.onCreate`) without needing an emulator or device.

**Steps to replicate in another Android project:**

1. Add to `libs.versions.toml`:
   ```toml
   robolectric       = "4.14.1"
   androidx-test-ext = "1.2.1"
   ```
   And library entries for `robolectric`, `androidx-test-ext-junit`, `hilt-android-testing`.

2. Add to `app/build.gradle.kts`:
   ```kotlin
   android {
       testOptions { unitTests { isIncludeAndroidResources = true } }
   }
   dependencies {
       testImplementation(libs.robolectric)
       testImplementation(libs.androidx.test.ext.junit)
       testImplementation(libs.hilt.android.testing)
       kspTest(libs.hilt.compiler)
   }
   ```

3. Create a `HiltTestApp.kt` in `src/test`:
   ```kotlin
   @CustomTestApplication(YourApp::class)
   interface HiltTestApp
   ```

4. Create `MainActivityStartupTest.kt` in `src/test`:
   ```kotlin
   @HiltAndroidTest
   @RunWith(AndroidJUnit4::class)
   @Config(application = HiltTestApp_Application::class, sdk = [34])
   class MainActivityStartupTest {
       @get:Rule val hiltRule = HiltAndroidRule(this)

       @Test
       fun activityStartsWithoutCrash() {
           ActivityScenario.launch(MainActivity::class.java).use { scenario ->
               scenario.onActivity { check(!it.isFinishing) }
           }
       }
   }
   ```

5. If the Activity casts `application as YourApp`, make it a safe cast (`as?`) so the
   test application (`HiltTestApp_Application`) doesn't cause a `ClassCastException`.
