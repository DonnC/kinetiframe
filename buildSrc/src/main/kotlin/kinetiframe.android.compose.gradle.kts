// Compose for an Android library or app: the compiler plugin, the BOM and preview tooling.
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasHostTestsBuilder
import com.android.build.api.variant.HostTestBuilder

plugins { id("org.jetbrains.kotlin.plugin.compose") }

extensions.configure<CommonExtension> {
    buildFeatures.compose = true
    // AGP 9.5 creates a Compose Preview task in every Compose module, and creating it fails unless
    // host tests include Android resources. The command line never creates that task, but IDE imports
    // create every task, so without this they fail and retry.
    testOptions.unitTests.isIncludeAndroidResources = true
}
// The same task needs a host test variant for every build type, and AGP 9 makes one only for debug.
(extensions.getByName("androidComponents") as AndroidComponentsExtension<*, *, *>).beforeVariants { variant ->
    (variant as? HasHostTestsBuilder)?.hostTests?.get(HostTestBuilder.UNIT_TEST_TYPE)?.enable = true
}
dependencies {
    val bom = platform(libs.library("androidx-compose-bom"))
    add("implementation", bom)
    add("androidTestImplementation", bom)
    // Compose's test library brings an older Espresso that calls InputManager.getInstance, which Android 16
    // removed, so device tests fail there without a current one.
    add("androidTestImplementation", libs.library("androidx-espresso-core"))
    add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
    add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
    add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
}
