// SPDX-License-Identifier: Apache-2.0
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

/** Settings every KinetiFrame module shares. The convention plugins apply them. */
internal object KinetiFrameBuild {
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    const val MIN_SDK = 28
    val JAVA = JavaVersion.VERSION_11
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String) = findLibrary(alias).get()

/** JUnit 5 for host tests. JUnit 4 tests run through the vintage engine where a module adds it. */
internal fun Project.useJUnit5() {
    dependencies {
        add("testImplementation", platform(libs.library("junit5-bom")))
        add("testImplementation", libs.library("junit5-jupiter"))
        add("testRuntimeOnly", libs.library("junit5-launcher"))
    }
    tasks.withType<Test>().configureEach { useJUnitPlatform() }
}
