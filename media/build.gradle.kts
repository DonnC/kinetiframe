plugins {
    id("kinetiframe.android.library")
}
android { namespace = "com.donnclab.kinetiframe.media" }
dependencies {
    api(project(":core"))
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
}
