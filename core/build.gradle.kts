plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) } }
dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.core)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(platform(libs.junit5.bom))
    testImplementation(libs.junit5.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit5.launcher)
}
