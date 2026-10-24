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
    api(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
}
