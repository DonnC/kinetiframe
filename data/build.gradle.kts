plugins { alias(libs.plugins.android.library) }
android { namespace = "com.donnclab.kinetiframe.data" }
dependencies {
    implementation(project(":core"))
    testImplementation(platform(libs.junit5.bom))
    testImplementation(libs.junit5.jupiter)
    testRuntimeOnly(libs.junit5.launcher)
}
