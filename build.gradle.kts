plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.androidLint) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.firebaseAppDistribution) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// add-lint-quality-gates: one ktlint version for every project (root included, for its own *.gradle.kts).
allprojects {
    pluginManager.withPlugin("org.jlleitschuh.gradle.ktlint") {
        configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            version.set(rootProject.libs.versions.ktlint.get())
            // Generated sources (SQLDelight, Metro, Compose resources) live under build/ and aren't ours to format.
            filter { exclude { it.file.path.contains("${java.io.File.separator}build${java.io.File.separator}") } }
        }
    }
}

// add-lint-quality-gates: one root detekt task over every module's sources, every KMP source set included (the plugin's
// own default only looks at src/main and src/test), without type resolution (design.md decision 2).
detekt {
    source.setFrom(
        subprojects.map { it.layout.projectDirectory.dir("src") } +
            fileTree(rootDir) { include("*.gradle.kts", "*/*.gradle.kts") },
    )
    basePath.set(rootDir)
    buildUponDefaultConfig.set(true)
    config.setFrom(file("config/detekt/detekt.yml"))
    // Findings that predate the gate; may only shrink (docs/code-quality.md, scripts/check-baselines.sh).
    baseline.set(file("config/detekt/baseline.xml"))
}

// add-lint-quality-gates: the static analysis gate, part of the final regression run (docs/code-quality.md). Not hooked into
// `check`, which on a KMP module also runs every target's tests, iOS included (design.md decision 5).
tasks.register("codeQuality") {
    group = "verification"
    description = "Runs ktlint, detekt and Android lint (both app flavors and the shared module)."
    dependsOn(
        allprojects.map { "${it.path.removeSuffix(":")}:ktlintCheck" },
        "detekt",
        ":androidApp:lintFakeDebug",
        ":androidApp:lintProductionDebug",
        ":shared:lintAndroidMain",
    )
}
