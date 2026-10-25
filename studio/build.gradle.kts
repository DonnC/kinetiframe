plugins {
    id("kinetiframe.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}
android {
    namespace = "com.donnclab.kinetiframe.studio"
    defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    // Drawing and render tests touch android.graphics types, as they did in app.
    testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies {
    // The studio's state and managers expose core documents, actors and media in their API.
    api(project(":core"))
    api(project(":engine"))
    api(project(":data"))
    api(project(":media"))
    api(project(":actors"))
    // Strokes and selections still use Compose's Offset, Path and Color; the compose check counts each use.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    // Runs the existing JUnit 4 tests alongside JUnit 5 ones.
    testRuntimeOnly(libs.junit5.vintage)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
}
