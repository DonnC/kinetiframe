plugins { alias(libs.plugins.android.library) }
android { namespace = "com.donnclab.kinetiframe.presentation" }
dependencies {
    api(project(":core"))
    implementation(project(":engine"))
    implementation(project(":designsystem"))
    api(libs.androidx.lifecycle.viewmodel)
}
