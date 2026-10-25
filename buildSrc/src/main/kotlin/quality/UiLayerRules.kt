// SPDX-License-Identifier: Apache-2.0
package quality

/**
 * Keeps the UI replaceable on its own, as the feature modules decision requires:
 *
 * - `compose`: Compose imports in product rules and storage, meaning packages with a `domain`,
 *   `data`, `render`, `rig`, `math` or `model` segment. How something is drawn must not shape
 *   the rules behind it.
 * - `uidata`: imports of a `data` package from UI code. Screens reach storage only through their
 *   ViewModel. A ViewModel may use the repositories in `media`, `actors` and `studio`, but not the
 *   `data` module underneath them.
 *
 * Both are counted per file in the main code of every module, so code keeps its findings when it
 * moves out of the app.
 */
internal object UiLayerRules {
    const val COMPOSE = "compose"
    const val UI_DATA = "uidata"

    private const val ROOT = "com.donnclab.kinetiframe."
    private val NON_UI_SEGMENTS = setOf("domain", "data", "render", "rig", "math", "model")

    // Screen packages that predate the `ui` subpackage convention.
    private val SCREEN_PACKAGES = setOf("studio.editor", "home", "settings")

    fun applies(path: String): Boolean = "/src/main/" in path

    /** The rule an import from [packageName] breaks, or null. [viewModel] marks a ViewModel's file. */
    fun violation(packageName: String, import: String, viewModel: Boolean = false): String? {
        val segments = packageName.removePrefix(ROOT).split('.')
        return when {
            import.startsWith("androidx.compose.") && segments.any { it in NON_UI_SEGMENTS } -> COMPOSE
            isUi(packageName, segments) && reachesStorage(import, viewModel) -> UI_DATA
            else -> null
        }
    }

    /** Whether [path] is a ViewModel's file, by the `*ViewModel.kt` naming convention. */
    fun isViewModel(path: String): Boolean = path.endsWith("ViewModel.kt")

    private fun reachesStorage(import: String, viewModel: Boolean): Boolean = when {
        !import.startsWith(ROOT) -> false
        viewModel -> import.startsWith("${ROOT}data.")
        else -> import.removePrefix(ROOT).split('.').dropLast(1).contains("data")
    }

    private fun isUi(packageName: String, segments: List<String>): Boolean =
        "ui" in segments || segments.first() == "presentation" || packageName.removePrefix(ROOT) in SCREEN_PACKAGES
}
