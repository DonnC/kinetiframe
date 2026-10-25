// Android library: the shared SDK levels, Java 11, JUnit 5 host tests and lint baselines.
plugins { id("com.android.library") }

android {
    compileSdk = KinetiFrameBuild.COMPILE_SDK
    defaultConfig { minSdk = KinetiFrameBuild.MIN_SDK }
    compileOptions {
        sourceCompatibility = KinetiFrameBuild.JAVA
        targetCompatibility = KinetiFrameBuild.JAVA
    }
    lint {
        // Each module records its existing warnings, as app does, so new issues stand out.
        baseline = file("lint-baseline.xml")
        // Dependabot reports dependency updates; these checks change with every upstream release.
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }
}
useJUnit5()
