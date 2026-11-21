plugins { `kotlin-dsl` }
dependencies {
    // Gradle plugins the convention plugins in src/main/kotlin apply, at the catalog's versions.
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation(
        "org.jetbrains.kotlin:kotlin-serialization:${libs.versions.jetbrainsKotlinPluginSerialization.get()}",
    )
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:${libs.versions.googleDevtoolsKsp.get()}")
    // Build tooling only. Use the existing compiler version, not a new runtime lint SDK.
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")
    testImplementation(platform(libs.junit5.bom))
    testImplementation(libs.junit5.jupiter)
    testRuntimeOnly(libs.junit5.launcher)
}
tasks.test { useJUnitPlatform() }
