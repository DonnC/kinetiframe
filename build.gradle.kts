// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization) apply false
    alias(libs.plugins.spotless)
}

// ktlint formatting for every Kotlin source and build script. Rules and line length come from
// .editorconfig. Run `./gradlew spotlessApply` to fix formatting before committing.
spotless {
    kotlin {
        target("**/src/**/*.kt")
        // Formatting these would take them past the 500-line limit or their cap in
        // .architecture/legacy-baseline.properties. Each one is formatted when it is split.
        targetExclude(
            "app/src/main/java/com/donnclab/kinetiframe/studio/EditorScreen.kt",
            "app/src/main/java/com/donnclab/kinetiframe/studio/StudioViewModel.kt",
        )
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("*.gradle.kts", "*/*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
}

// One convention entry point for the extracted Android libraries; no per-module version drift.
subprojects {
    pluginManager.withPlugin("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            compileSdk = 37
            defaultConfig { minSdk = 28 }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
        }
    }
    tasks.withType<Test>().configureEach { useJUnitPlatform() }
}

val architectureParser by configurations.creating
dependencies { architectureParser("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}") }
// -PengineOnly=true builds only core, engine and the sandbox app (see settings.gradle.kts).
// -PpublicOnly=true is the old name for the same option and still works.
val engineOnly = providers.gradleProperty("engineOnly").orElse(providers.gradleProperty("publicOnly")).orNull == "true"
val auditedModules = if (engineOnly) {
    listOf("core", "engine", "sandbox")
} else {
    listOf("app", "core", "engine", "data", "presentation", "designsystem", "sandbox")
}
tasks.register<quality.ArchitectureCheckTask>("checkArchitecture") {
    repository.set(layout.projectDirectory)
    modules.set(auditedModules)
    reportOnly.set(false)
    parserClasspath.from(architectureParser)
}
tasks.register<quality.ArchitectureCheckTask>("reportArchitectureDebt") {
    repository.set(layout.projectDirectory)
    modules.set(auditedModules)
    reportOnly.set(true)
    parserClasspath.from(architectureParser)
}

// One command for contributors and CI: formatting, architecture rules, host tests and Android lint.
// Device tests (connectedDebugAndroidTest) still need an emulator or phone and run separately.
tasks.register("verify") {
    group = "verification"
    description = "Runs formatting, the architecture check, host tests and lint for the modules in this build."
    dependsOn(
        "spotlessCheck",
        "checkArchitecture",
        ":core:test",
        ":engine:testDebugUnitTest",
        ":sandbox:testDebugUnitTest",
    )
    if (!engineOnly) {
        dependsOn(
            ":data:testDebugUnitTest",
            ":designsystem:testDebugUnitTest",
            ":app:testDebugUnitTest",
            ":app:lintDebug",
        )
    }
}
