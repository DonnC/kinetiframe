// Android app: the shared SDK levels, Java 11, the instrumentation runner and JUnit 5 host tests.
plugins { id("com.android.application") }

android {
    compileSdk = KinetiFrameBuild.COMPILE_SDK
    defaultConfig {
        minSdk = KinetiFrameBuild.MIN_SDK
        targetSdk = KinetiFrameBuild.TARGET_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = KinetiFrameBuild.JAVA
        targetCompatibility = KinetiFrameBuild.JAVA
    }
}
useJUnit5()
