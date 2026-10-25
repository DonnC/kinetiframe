plugins {
    id("kinetiframe.android.library")
}
android { namespace = "com.donnclab.kinetiframe.media" }
dependencies {
    api(project(":core"))
    // Scene video geometry still uses Compose's Offset and Rect; the compose check counts each use.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
}
