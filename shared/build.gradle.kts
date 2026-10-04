plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.metro)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.androidLint)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.mikonoma.drivinglog.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTest {}

        // add-lint-quality-gates: the app's checkDependencies doesn't reach a KMP library's sources, so `shared` is linted
        // on its own (`lintAndroidMain`, enabled by the com.android.lint plugin above), with the app's settings (design.md
        // decision 4).
        lint {
            warningsAsErrors = true
            abortOnError = true
            // These compare against what's published online, so they'd fail the gate on a new upstream release with no code
            // change. Dependency updates are a deliberate change of their own, not a lint finding.
            disable += setOf("NewerVersionAvailable", "GradleDependency", "AndroidGradlePluginVersion")
            // targetSdk is the app's setting (a library has none); the app's lint reports it, against its own build file.
            disable += "OldTargetApi"
        }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            linkerOpts.add("-lsqlite3")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.icons.core)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.navigationevent.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            api(libs.kotlinx.io.core)
            implementation(libs.coil.compose)
            implementation(libs.material.color.utilities)
            api(libs.kide)
            implementation(libs.kide.navigation)
            implementation(libs.kotlinx.serialization.core)
            // add-odometer-ocr-capture: a scan's detection result is stored as JSON in event_capture.detections.
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.androidx.activity.compose)
            // add-odometer-ocr-capture: the bundled (not Play Services) model, so OCR works offline from the first scan.
            implementation(libs.mlkit.text.recognition)
            // add-seven-segment-ocr: PP-OCR models (seven-segment LCD digits) run on ONNX Runtime.
            implementation(libs.onnxruntime.android)
            // add-live-scanner: the in-app camera preview and frame analysis.
            implementation(libs.camerax.core)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.camerax.view)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kide.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.sqldelight.sqlite.driver)
            // The same ai.onnxruntime API on the JVM, so the PP-OCR recognizer runs over the real photos without an emulator.
            implementation(libs.onnxruntime.jvm)
        }
    }
}

sqldelight {
    databases {
        create("DrivingLogDatabase") {
            packageName.set("com.mikonoma.drivinglog.db")
        }
    }
}

// GenerateFixturesTest is a fixture-writing tool run only via :shared:generateMaestroFixtures (below), never as
// part of the ordinary test suite: it needs a system property that task alone sets, and it writes files as a side
// effect, which no other test should do. `withType(...).configureEach` (not `tasks.named`) since AGP registers
// "testAndroidHostTest" lazily, after this script's own top-level statements already ran.
//
// The ordinary suite gets the fixtures directory too (as maestroFixturesDir), for FixtureFreshnessTest: it opens
// every checked-in maestro/assets/fixtures/*.db and asserts its PRAGMA user_version still matches
// DrivingLogDatabase.Schema.version, so a migration that outpaces the checked-in fixtures fails fast, here, in a
// unit test - not later, mysteriously, in a slow Maestro flow on a device.
// The Android ONNX Runtime's classes and the JVM one's are the same API; the host tests run on the JVM one (its native
// library loads there, the Android one's cannot), so the Android artifact is kept off their runtime classpath.
configurations.matching { it.name.startsWith("androidHostTest") && it.name.endsWith("RuntimeClasspath") }.configureEach {
    exclude(group = "com.microsoft.onnxruntime", module = "onnxruntime-android")
}

tasks.withType<Test>().configureEach {
    if (name == "testAndroidHostTest") {
        filter { excludeTestsMatching("com.mikonoma.drivinglog.vehicle.fixtures.GenerateFixturesTest") }
    }
    systemProperty("maestroFixturesDir", rootDir.resolve("maestro/assets/fixtures").absolutePath)
    // add-seven-segment-ocr: the PP-OCR models the app ships, and the photos they are tested over.
    systemProperty("ocrModelsDir", rootDir.resolve("androidApp/src/main/assets/ocr").absolutePath)
    systemProperty("ocrPhotosDir", rootDir.resolve("maestro/assets/ocr").absolutePath)
    // The SQLite JDBC driver loads its native library; JDK 25 warns unless native access is enabled (fix-build-warnings).
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// speed-up-tests-with-db-fixtures: regenerates maestro/assets/fixtures/*.db by running the real repository code
// (GenerateFixtures.kt, an androidHostTest test that writes files as a side effect rather than asserting) on the
// JVM. Run on demand when a fixture's scenario changes or the schema migrates; its output is checked in, not
// regenerated on every build. Reuses testAndroidHostTest's own already-resolved classpath (rather than resolving
// the compilation's dependencies again as a plain JavaExec, which hits AGP variant-ambiguity errors) filtered down
// to just this one test.
tasks.register<Test>("generateMaestroFixtures") {
    group = "verification"
    description = "Regenerates maestro/assets/fixtures/*.db from the real repository code (JVM)."
    val hostTest = tasks.named<Test>("testAndroidHostTest").get()
    dependsOn(hostTest.taskDependencies.getDependencies(hostTest))
    testClassesDirs = hostTest.testClassesDirs
    classpath = hostTest.classpath
    filter { includeTestsMatching("com.mikonoma.drivinglog.vehicle.fixtures.GenerateFixturesTest") }
    systemProperty("maestroFixturesDir", rootDir.resolve("maestro/assets/fixtures").absolutePath)
    outputs.upToDateWhen { false }
    testLogging { showStandardStreams = true }
}
