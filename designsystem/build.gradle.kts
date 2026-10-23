plugins {
    id("kinetiframe.android.library")
    id("kinetiframe.android.compose")
}

android {
    namespace = "com.donnclab.kinetiframe.designsystem"
    defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.lucide.icons)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
}
