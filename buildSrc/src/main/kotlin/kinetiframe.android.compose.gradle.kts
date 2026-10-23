// Compose for an Android library or app: the compiler plugin, the BOM and preview tooling.
import com.android.build.api.dsl.CommonExtension

plugins { id("org.jetbrains.kotlin.plugin.compose") }

extensions.configure<CommonExtension> { buildFeatures.compose = true }
dependencies {
    val bom = platform(libs.library("androidx-compose-bom"))
    add("implementation", bom)
    add("androidTestImplementation", bom)
    add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
    add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
    add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
}
