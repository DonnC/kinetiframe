// SPDX-License-Identifier: Apache-2.0
package quality

/**
 * Keeps the UI replaceable on its own, as the feature modules decision requires:
 *
 * - `compose`: Compose imports in product rules and storage, meaning packages with a `domain`,
 *   `data`, `render`, `rig`, `math` or `model` segment. How something is drawn must not shape
 *   the rules behind it.
 * - `uidata`: imports of a `data` package from UI code, which reaches storage only through its
 *   ViewModel.
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

    /** The rule an import from [packageName] breaks, or null. */
    fun violation(packageName: String, import: String): String? {
        val segments = packageName.removePrefix(ROOT).split('.')
        return when {
            import.startsWith("androidx.compose.") && segments.any { it in NON_UI_SEGMENTS } -> COMPOSE

            isUi(packageName, segments) && import.startsWith(ROOT) &&
                import.removePrefix(ROOT).split('.').dropLast(1).contains("data") -> UI_DATA

            else -> null
        }
    }

    private fun isUi(packageName: String, segments: List<String>): Boolean =
        "ui" in segments || packageName.removePrefix(ROOT) in SCREEN_PACKAGES
}
