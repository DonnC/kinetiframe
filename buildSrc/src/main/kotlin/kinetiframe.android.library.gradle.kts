// Android library: the shared SDK levels, Java 11 and JUnit 5 host tests.
plugins { id("com.android.library") }

android {
    compileSdk = KinetiFrameBuild.COMPILE_SDK
    defaultConfig { minSdk = KinetiFrameBuild.MIN_SDK }
    compileOptions {
        sourceCompatibility = KinetiFrameBuild.JAVA
        targetCompatibility = KinetiFrameBuild.JAVA
    }
}
useJUnit5()
