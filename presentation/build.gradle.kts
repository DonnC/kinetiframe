plugins {
    id("kinetiframe.android.library")
    id("kinetiframe.android.compose")
}
android {
    namespace = "com.donnclab.kinetiframe.presentation"
    defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
}
dependencies {
    api(project(":core"))
    implementation(project(":engine"))
    implementation(project(":designsystem"))
    // Screens drive the logic modules through their ViewModels.
    implementation(project(":media"))
    implementation(project(":actors"))
    api(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.lucide.icons)
    implementation(libs.coil.compose)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
}
