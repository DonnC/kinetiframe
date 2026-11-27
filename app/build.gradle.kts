plugins {
    id("kinetiframe.android.application")
    id("kinetiframe.android.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.donnclab.kinetiframe"

    defaultConfig {
        applicationId = "com.donnclab.kinetiframe"
        versionCode = 2
        versionName = "0.1.1"
    }

    androidResources {
        // Lists the app's languages for Android's per-app language setting, from the values-*
        // folders. resources.properties names the default folder's language.
        generateLocaleConfig = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            // en-XA (long, accented text) and ar-XB (right to left) for checking layouts.
            isPseudoLocalesEnabled = true
        }
        release {
            // R8 shrinks the code to what the app uses: the unshrunk build shipped 63 MB of
            // dex, mostly unused icons and library classes.
            optimization {
                enable = true
            }
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    lint {
        // Existing warnings are recorded in lint-baseline.xml so new issues stand out.
        // Regenerate it with ./gradlew :app:updateLintBaseline only when removing entries.
        baseline = file("lint-baseline.xml")
        checkDependencies = false
        // Dependabot reports dependency updates. These checks change with every upstream release,
        // which would make the same commit pass one day and report new issues the next.
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":engine"))
    implementation(project(":data"))
    implementation(project(":media"))
    implementation(project(":actors"))
    implementation(project(":studio"))
    implementation(project(":presentation"))
    implementation(project(":designsystem"))
    testImplementation(libs.robolectric)
    // Runs the existing JUnit 4 tests alongside JUnit 5 ones.
    testRuntimeOnly(libs.junit5.vintage)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.material)
    implementation(libs.compose.colorpicker)
    implementation(libs.lucide.icons)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
}

tasks.withType<Test>().configureEach {
    // Host-side archive/render tests read these directly, not via Android resources.
    // A changed asset fixture must invalidate test results even when Kotlin is unchanged.
    inputs.dir("src/main/assets/packs").withPropertyName("packFixtures")
    inputs.dir("src/main/assets/samples").withPropertyName("sampleFixtures")
    inputs.file("src/main/assets/actor/starter-actors.zip").withPropertyName("starterActorFixtures")
    inputs.dir("src/main/assets/library").withPropertyName("libraryFixtures")
    // Required by Robolectric on Java 17+; production and instrumentation VMs are unaffected.
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.net=ALL-UNNAMED",
        "--add-opens=java.base/java.security=ALL-UNNAMED",
        "--add-opens=java.base/java.text=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    )
}
