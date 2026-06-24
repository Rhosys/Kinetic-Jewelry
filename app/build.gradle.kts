import java.time.Instant

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val gitCommitHash =
    providers
        .exec { commandLine("git", "rev-parse", "--short=8", "HEAD") }
        .standardOutput.asText.get()
        .trim()

val buildTimestamp = Instant.now().toString()

// Package the repo-root ble-protocol.json into app assets so the app reads
// the exact same BLE UUIDs as the firmware (which embeds the same file at
// compile time via firmware/protocol/build.rs) instead of hand-synced literals.
val bleProtocolAssetsDir = layout.buildDirectory.dir("generated/assets/protocol")

val copyBleProtocolJson by tasks.registering(Copy::class) {
    from(rootProject.file("ble-protocol.json"))
    into(bleProtocolAssetsDir)
}

tasks.matching { Regex("merge\\w*Assets").matches(it.name) }.configureEach {
    dependsOn(copyBleProtocolJson)
}

android {
    namespace = "ch.rhosys.lyra"
    compileSdk = 35

    defaultConfig {
        applicationId = "ch.rhosys.lyra"
        minSdk = 29
        targetSdk = 35
        versionCode = (findProperty("versionCode") as? String)?.toIntOrNull() ?: 1
        versionName = (findProperty("versionName") as? String) ?: "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GIT_COMMIT", "\"$gitCommitHash\"")
        buildConfigField("String", "BUILD_TIME", "\"$buildTimestamp\"")
    }

    signingConfigs {
        create("sharedDebug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            // TODO: re-enable once PostHog LocalLifecycleOwner crash is confirmed fixed
            isMinifyEnabled = false
            // proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sign release with the shared debug key so the minified variant is
            // installable on a local emulator (npm run start:release) to catch
            // R8 stripping crashes before they reach CI. The Play Store upload is
            // signed separately via android-upload-signing.keystore + Play App
            // Signing — this config does not affect the published artifact.
            signingConfig = signingConfigs.getByName("sharedDebug")
        }
        debug {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("sharedDebug")
        }
    }

    // Run instrumented tests against release to catch R8 stripping issues
    testBuildType = "release"

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(bleProtocolAssetsDir)
        }
        getByName("debug") {
            java.srcDirs("src/debug/java")
        }
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.posthog.android)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
    wearApp(project(":wear"))

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.room.testing)
    testImplementation("org.json:json:20240303")
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Pass the canonical test-vectors path to JVM unit tests so ProtocolRoundtripTest
// can locate the file without guessing relative paths.
tasks.withType<Test> {
    systemProperty(
        "protocolVectorsPath",
        "${rootProject.projectDir}/firmware/protocol/tests/fixtures/test-vectors.json",
    )
}
