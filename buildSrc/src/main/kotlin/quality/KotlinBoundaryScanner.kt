// SPDX-License-Identifier: Apache-2.0
package quality

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import java.io.File

/**
 * Checks file size, parameter counts and module import boundaries using the Kotlin PSI, so commas
 * in generics, lambdas and strings are never mistaken for parameters.
 *
 * Parameter limits follow common Kotlin linting defaults: functions and lambdas take at most
 * [MAX_FUNCTION_PARAMETERS], constructors at most [MAX_CONSTRUCTOR_PARAMETERS]. Data class
 * constructors are exempt because their parameters are the data. In `@Composable` functions only
 * parameters without a default value count, so optional `modifier` and styling parameters, which
 * the Compose API guidelines ask for, never push a component over the limit.
 */
internal object KotlinBoundaryScanner {
    fun scan(root: File, modules: List<String>): Map<String, Int> {
        val violations = sortedMapOf<String, Int>()
        val disposable = Disposer.newDisposable()
        try {
            val environment = KotlinCoreEnvironment.createForProduction(
                disposable,
                CompilerConfiguration(),
                EnvironmentConfigFiles.JVM_CONFIG_FILES,
            )
            val factory = KtPsiFactory(environment.project, false)
            modules.forEach { module ->
                File(root, "$module/src").walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { source ->
                    val path = source.relativeTo(root).invariantSeparatorsPath
                    val text = source.readText()
                    val lines = text.lines().size
                    if (lines > MAX_LINES) violations["lines|$path"] = lines
                    val parsed = factory.createFile(source.name, text.replace("\r\n", "\n").replace('\r', '\n'))
                    val pkg = parsed.packageFqName.asString()
                    if (module == "app" && RETIRED_APP_PACKAGES.any { pkg == it || pkg.startsWith("$it.") }) {
                        violations["package|$path|$pkg"] = 1
                    }
                    val layer = FeatureLayering.featureOf(pkg)?.takeIf { FeatureLayering.applies(module, path) }
                    val layering = sortedMapOf<String, Int>()
                    fun checkLayering(reference: String) {
                        val target = layer?.let { FeatureLayering.violation(it, reference) } ?: return
                        layering[target] = (layering[target] ?: 0) + 1
                    }
                    if (UiValueRules.applies(module, path)) {
                        UiValueRules.count(parsed).forEach { (kind, count) -> violations["$kind|$path"] = count }
                    }
                    parsed.accept(object : KtTreeVisitorVoid() {
                        override fun visitNamedFunction(function: KtNamedFunction) {
                            val composable = function.annotationEntries.any { it.shortName?.asString() == "Composable" }
                            val counted = if (composable) {
                                function.valueParameters.count { !it.hasDefaultValue() }
                            } else {
                                function.valueParameters.size
                            }
                            inspect(
                                function,
                                function.name ?: "anonymous",
                                function.valueParameters,
                                counted,
                                MAX_FUNCTION_PARAMETERS,
                            )
                            super.visitNamedFunction(function)
                        }
                        override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
                            val owner = constructor.getContainingClassOrObject()
                            if (!(owner is KtClass && owner.isData())) {
                                inspect(
                                    constructor,
                                    "constructor",
                                    constructor.valueParameters,
                                    constructor.valueParameters.size,
                                    MAX_CONSTRUCTOR_PARAMETERS,
                                )
                            }
                            super.visitPrimaryConstructor(constructor)
                        }
                        override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
                            inspect(
                                constructor,
                                "constructor",
                                constructor.valueParameters,
                                constructor.valueParameters.size,
                                MAX_CONSTRUCTOR_PARAMETERS,
                            )
                            super.visitSecondaryConstructor(constructor)
                        }
                        override fun visitLambdaExpression(expression: KtLambdaExpression) {
                            inspect(
                                expression,
                                "lambda",
                                expression.valueParameters,
                                expression.valueParameters.size,
                                MAX_FUNCTION_PARAMETERS,
                            )
                            super.visitLambdaExpression(expression)
                        }
                        override fun visitImportDirective(directive: KtImportDirective) {
                            checkReference(directive.importedFqName?.asString().orEmpty())
                            checkLayering(directive.importedFqName?.asString().orEmpty())
                            super.visitImportDirective(directive)
                        }
                        override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                            // Imports are checked once as complete names, not again as partial qualifiers.
                            if (generateSequence(expression.parent) { it.parent }.none { it is KtImportDirective }) {
                                checkReference(expression.text)
                                if (expression.parent !is KtDotQualifiedExpression) checkLayering(expression.text)
                            }
                            super.visitDotQualifiedExpression(expression)
                        }

                        // The key lists every declared parameter type so a baseline entry identifies one
                        // exact signature; the value is the counted parameters.
                        private fun inspect(
                            element: KtElement,
                            name: String,
                            parameters: List<KtParameter>,
                            counted: Int,
                            limit: Int,
                        ) {
                            if (counted <= limit) return
                            val owners = generateSequence(element.parent) {
                                it.parent
                            }.filterIsInstance<KtNamedDeclaration>()
                                .mapNotNull { it.name }.toList().reversed().joinToString(".")
                            val types = parameters.joinToString(",") {
                                it.typeReference?.text.orEmpty().filterNot(Char::isWhitespace)
                            }
                            violations["arity|$path|$owners.$name($types)"] = counted
                        }
                        private fun checkReference(reference: String) {
                            val forbidden =
                                forbiddenPrefixes(module, path).firstOrNull { reference.startsWith(it) } ?: return
                            violations["dependency|$path|$forbidden"] = 1
                        }
                    })
                    layering.forEach { (target, count) -> violations["layering|$path|$target"] = count }
                }
            }
        } finally {
            Disposer.dispose(disposable)
        }
        return violations
    }

    private fun forbiddenPrefixes(module: String, path: String): List<String> =
        if (module == "app") emptyList() else moduleRules(module, path) + APP_PACKAGES

    private fun moduleRules(module: String, path: String): List<String> {
        val sdk = listOf("com.google.firebase.", "com.android.billingclient.", "io.appwrite.")
        return when (module) {
            "core" ->
                sdk +
                    listOf(
                        "android.",
                        "androidx.",
                        "com.donnclab.kinetiframe.ui.",
                        "com.donnclab.kinetiframe.data.",
                        "com.donnclab.kinetiframe.engine.",
                        "com.donnclab.kinetiframe.designsystem.",
                    )

            "engine" ->
                sdk +
                    listOf(
                        "androidx.compose.",
                        "com.donnclab.kinetiframe.ui.",
                        "com.donnclab.kinetiframe.data.",
                        "com.donnclab.kinetiframe.designsystem.",
                    ) +
                    if (path.contains("/math/")) listOf("android.", "androidx.", "java.io.") else emptyList()

            "data" -> listOf(
                "androidx.compose.",
                "com.donnclab.kinetiframe.ui.",
                "com.donnclab.kinetiframe.engine.",
                "com.donnclab.kinetiframe.designsystem.",
            )

            "presentation" -> listOf("com.donnclab.kinetiframe.data.")

            "designsystem" ->
                sdk +
                    listOf(
                        "com.donnclab.kinetiframe.core.",
                        "com.donnclab.kinetiframe.engine.",
                        "com.donnclab.kinetiframe.data.",
                        "com.donnclab.kinetiframe.ui.",
                        "com.donnclab.kinetiframe.presentation.",
                        "com.donnclab.kinetiframe.domain.",
                        "com.donnclab.kinetiframe.models.",
                        "androidx.lifecycle.",
                    )

            "sandbox" ->
                sdk +
                    listOf(
                        "com.donnclab.kinetiframe.ui.",
                        "com.donnclab.kinetiframe.data.",
                        "com.donnclab.kinetiframe.presentation.",
                        "com.donnclab.kinetiframe.designsystem.",
                    )

            else -> emptyList()
        }
    }

    // Feature packages of the app module. Library modules sit below app and never reference them.
    private val APP_PACKAGES = listOf(
        "actors", "audio", "common", "diagnostics", "export", "home", "library", "media", "navigation",
        "preview", "project", "settings", "studio", "video",
    ).map { "com.donnclab.kinetiframe.$it." }

    // Layer-named packages the app module no longer uses. `engine`, `data` and `core` are module
    // names, and `ui`, `domain` and `models` group by layer instead of by feature.
    private val RETIRED_APP_PACKAGES = listOf("core", "data", "domain", "engine", "models", "ui")
        .map { "com.donnclab.kinetiframe.$it" }

    private const val MAX_LINES = 500
    const val MAX_FUNCTION_PARAMETERS = 6
    const val MAX_CONSTRUCTOR_PARAMETERS = 7
}
