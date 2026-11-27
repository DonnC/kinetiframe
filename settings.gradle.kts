pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "KinetiFrame"
include(":app")

// Every module except app lives in modules/. Project paths stay short: :core, not :modules:core.
val modules = listOf("core", "engine", "data", "media", "actors", "studio", "presentation", "designsystem")
modules.forEach { name ->
    include(":$name")
    project(":$name").projectDir = file("modules/$name")
}
