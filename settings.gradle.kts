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
include(":core", ":engine", ":sandbox")
// -PengineOnly=true skips the Android app and its UI modules, so engine contributors get a small,
// fast build of core, engine and the sandbox app. -PpublicOnly=true is the old name and still works.
val engineOnly = providers.gradleProperty("engineOnly").orElse(providers.gradleProperty("publicOnly")).orNull == "true"
if (!engineOnly) {
    include(":app", ":data", ":presentation", ":designsystem")
}
