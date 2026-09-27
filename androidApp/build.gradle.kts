import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.firebaseAppDistribution)
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
                // appId/groups are tracked, non-secret placeholders in gradle.properties until the one-time
                // Firebase setup fills in real values (see design.md's appId decision). releaseNotesFile has no
                // default: the caller must always supply -PdistributionReleaseNotesFile=<path>.
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
    // before producing ... any artifact". `packageRelease` (the APK) and `packageReleaseBundle` (the AAB) are the
    // tasks that actually write it.
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
    tasks.matching { it.name == "packageRelease" || it.name == "packageReleaseBundle" }.configureEach {
        dependsOn(checkReleaseSigning)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
}
