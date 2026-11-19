// Plain Kotlin library with no Android: Java 17 bytecode and JUnit Jupiter tests.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins { id("org.jetbrains.kotlin.jvm") }

java {
    sourceCompatibility = KinetiFrameBuild.JAVA
    targetCompatibility = KinetiFrameBuild.JAVA
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
useJUnit5()
