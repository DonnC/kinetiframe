plugins {
    id("kinetiframe.android.library")
    id("kinetiframe.android.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}
android {
    namespace = "com.donnclab.kinetiframe.actors"
    // Some rig and render tests touch android.graphics.Matrix, as they did in app.
    testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies {
    // Repositories and renderers take core documents, engine rigs and data stores in their API.
    api(project(":core"))
    api(project(":engine"))
    api(project(":data"))
    implementation(project(":presentation"))
    implementation(project(":designsystem"))
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.lucide.icons)
    implementation(libs.coil.compose)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    // Runs the existing JUnit 4 tests alongside JUnit 5 ones.
    testRuntimeOnly(libs.junit5.vintage)
}
