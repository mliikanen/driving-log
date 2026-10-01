import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.firebaseAppDistribution)
    alias(libs.plugins.googleServices)
}

// versionCode/versionName are fully derived from git, never hand-edited (add-app-distribution/design.md).
val gitCommitCount = providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }.standardOutput.asText.get().trim().toInt()
val gitShortSha = providers.exec { commandLine("git", "rev-parse", "--short", "HEAD") }.standardOutput.asText.get().trim()

android {
    namespace = "com.mikonoma.drivinglog"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.mikonoma.drivinglog"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = gitCommitCount
        versionName = "$gitCommitCount-$gitShortSha"
    }

    // The on-device OCR libraries (ML Kit's, and ONNX Runtime's for add-seven-segment-ocr) are 20-40 MB of native code
    // per ABI. A release is for testers' phones, which are all arm64 at this minSdk; a debug build also runs on the
    // x86_64 emulator. Native libraries are stored compressed, which roughly halves them in the APK.
    buildTypes {
        getByName("debug") { ndk { abiFilters += listOf("arm64-v8a", "x86_64") } }
        getByName("release") { ndk { abiFilters += listOf("arm64-v8a") } }
    }
    packaging {
        jniLibs { useLegacyPackaging = true }
    }

    // add-firebase-auth: `production` is the real app (Firebase Auth, Credential Manager); `fake` is what
    // Maestro installs, with zero Firebase/Credential Manager dependency at all (design.md decision 6).
    flavorDimensions += "environment"
    productFlavors {
        create("production") { dimension = "environment" }
        create("fake") { dimension = "environment" }
    }
}

// The Google Services plugin requires google-services.json for every variant by default, with no built-in way to
// skip a flavor. The `fake` flavor never calls any Firebase API (design.md decision 6), so its processing task's
// output is simply unused — disabling it is correct, not a workaround for a missing file.
tasks.matching { it.name.startsWith("processFake") && it.name.endsWith("GoogleServices") }.configureEach {
    enabled = false
}

// Scoped to a local block, not top-level script `val`s: a `doFirst` closure over a script-level `val` captures a
// reference to the script object itself, which the configuration cache cannot serialize. Locals of this block are
// plain lambda captures instead, which it can.
run {
    // Both files are generated once (keytool) and kept outside the repo entirely, never gitignored-in-place — see
    // openspec/changes/add-app-distribution/design.md's signing decision.
    val keystorePropertiesFile = File(System.getProperty("user.home"), ".android-keystores/keystore.properties")
    val hasKeystoreProperties = keystorePropertiesFile.exists()
    val keystoreProperties = Properties().apply {
        if (hasKeystoreProperties) load(FileInputStream(keystorePropertiesFile))
    }

    android {
        signingConfigs {
            if (hasKeystoreProperties) {
                create("release") {
                    storeFile = file(keystoreProperties.getProperty("storeFile"))
                    storePassword = keystoreProperties.getProperty("storePassword")
                    keyAlias = keystoreProperties.getProperty("keyAlias")
                    keyPassword = keystoreProperties.getProperty("keyPassword")
                }
            }
        }

        buildTypes {
            release {
                isMinifyEnabled = false
                if (hasKeystoreProperties) {
                    signingConfig = signingConfigs.getByName("release")
                }
            }
        }

        // add-firebase-auth: scoped to the `production` flavor, not buildTypes.release, so an unqualified
        // `assembleRelease`/`appDistributionUploadRelease` never also builds or uploads `fakeRelease` (design.md
        // Risks) — appId/groups are tracked, non-secret placeholders in gradle.properties until the one-time
        // Firebase setup fills in real values (see design.md's appId decision). releaseNotesFile has no default:
        // the caller must always supply -PdistributionReleaseNotesFile=<path>.
        productFlavors {
            getByName("production") {
                firebaseAppDistribution {
                    appId = project.property("firebaseAppId") as String
                    groups = project.property("firebaseTesterGroup") as String
                    releaseNotesFile = project.findProperty("distributionReleaseNotesFile") as String? ?: ""
                }
            }
        }
    }

    // A guard task the actual artifact-writing tasks depend on, so it runs — and can fail — strictly before them.
    // `assembleRelease`/`bundleRelease` are lifecycle tasks: their own doFirst runs only after every dependency
    // (including the tasks that write the APK/AAB) has already executed, which is too late to satisfy "fails
    // before producing ... any artifact". `packageProductionRelease`/`packageFakeRelease` (the APK) and their
    // `...ReleaseBundle` counterparts (the AAB) are the tasks that actually write one — flavor-qualified since
    // AGP 9's product flavors mean "packageRelease" alone no longer exists (add-firebase-auth/design.md Risks).
    // Both flavors' release variant shares the same `signingConfig` above, so both still need this guard.
    val keystorePropertiesPath = keystorePropertiesFile.path
    val checkReleaseSigning = tasks.register("checkReleaseSigning") {
        doLast {
            if (!hasKeystoreProperties) {
                throw GradleException(
                    "Missing $keystorePropertiesPath — release signing is not configured. See docs/distribution.md."
                )
            }
        }
    }
    tasks.matching {
        it.name == "packageProductionRelease" || it.name == "packageProductionReleaseBundle" ||
            it.name == "packageFakeRelease" || it.name == "packageFakeReleaseBundle"
    }.configureEach {
        dependsOn(checkReleaseSigning)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // add-firebase-auth: real Google Sign-In only in the `production` flavor (design.md decisions 1, 2, 6) — the
    // `fake` flavor never links the Firebase SDK or Credential Manager at all.
    "productionImplementation"(platform(libs.firebase.bom))
    "productionImplementation"(libs.firebase.auth)
    "productionImplementation"(libs.androidx.credentials)
    "productionImplementation"(libs.androidx.credentials.play.services.auth)
    "productionImplementation"(libs.googleid)
}
