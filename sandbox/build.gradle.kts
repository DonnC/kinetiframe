plugins {
    id("kinetiframe.android.application")
    id("kinetiframe.android.compose")
}
android {
    namespace = "com.donnclab.kinetiframe.sandbox"
    defaultConfig {
        applicationId = "com.donnclab.kinetiframe.sandbox"
        versionCode = 1
        versionName = "0.1"
    }
}
dependencies {
    implementation(project(":core"))
    implementation(project(":engine"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
}
