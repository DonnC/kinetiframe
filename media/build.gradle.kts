plugins {
    id("kinetiframe.android.library")
    id("kinetiframe.android.compose")
}
android { namespace = "com.donnclab.kinetiframe.media" }
dependencies {
    api(project(":core"))
    implementation(project(":presentation"))
    implementation(project(":designsystem"))
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.lucide.icons)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
}
