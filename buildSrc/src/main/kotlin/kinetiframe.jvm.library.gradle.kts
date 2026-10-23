// Plain Kotlin library with no Android: Java 11 bytecode and JUnit 5 tests.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins { id("org.jetbrains.kotlin.jvm") }

java {
    sourceCompatibility = KinetiFrameBuild.JAVA
    targetCompatibility = KinetiFrameBuild.JAVA
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }
useJUnit5()
