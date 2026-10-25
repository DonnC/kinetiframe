// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.spotless)
}

// ktlint formatting for every Kotlin source and build script. Rules and line length come from
// .editorconfig. Run `./gradlew spotlessApply` to fix formatting before committing.
spotless {
    kotlin {
        target("**/src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("*.gradle.kts", "*/*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
}

val architectureParser by configurations.creating
dependencies { architectureParser("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}") }
val auditedModules = listOf("app", "core", "engine", "data", "media", "actors", "studio", "presentation", "designsystem")
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
    description = "Runs formatting, the architecture check, host tests and lint."
    dependsOn(
        "spotlessCheck",
        "checkArchitecture",
        ":core:test",
        ":engine:testDebugUnitTest",
        ":data:testDebugUnitTest",
        ":media:testDebugUnitTest",
        ":actors:testDebugUnitTest",
        ":studio:testDebugUnitTest",
        ":presentation:testDebugUnitTest",
        ":designsystem:testDebugUnitTest",
        ":app:testDebugUnitTest",
        ":app:lintDebug",
    )
}
