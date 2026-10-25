plugins {
    id("kinetiframe.android.library")
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
    // Rig geometry still uses Compose's Offset and Path; the compose check counts each use.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    // Runs the existing JUnit 4 tests alongside JUnit 5 ones.
    testRuntimeOnly(libs.junit5.vintage)
}
