plugins {
    id("kinetiframe.jvm.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}
dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.core)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
}
