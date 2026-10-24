plugins {
    id("kinetiframe.android.library")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}
android { namespace = "com.donnclab.kinetiframe.data" }
dependencies {
    // Saved project types and the Room database are part of this module's API.
    api(project(":core"))
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    implementation(libs.kotlinx.serialization.json)
    "ksp"(libs.androidx.room.compiler)
}
