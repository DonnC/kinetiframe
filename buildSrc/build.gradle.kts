plugins { `kotlin-dsl` }
dependencies {
    // Build tooling only. Use the existing compiler version, not a new runtime lint SDK.
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")
    testImplementation(platform(libs.junit5.bom))
    testImplementation(libs.junit5.jupiter)
    testRuntimeOnly(libs.junit5.launcher)
}
tasks.test { useJUnitPlatform() }
