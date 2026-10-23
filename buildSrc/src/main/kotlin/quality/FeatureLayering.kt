// SPDX-License-Identifier: Apache-2.0
package quality

/**
 * The order the app's feature packages are layered in, ahead of splitting them into modules. A
 * feature may use the features listed for it and everything they may use, never one above it.
 * Gradle rejects module cycles, so a package must follow these rules before it can become a
 * module of its own.
 *
 * Findings are counted per file and target feature, as `layering|<file>|<feature>`.
 */
internal object FeatureLayering {
    private const val ROOT = "com.donnclab.kinetiframe"

    private val direct: Map<String, Set<String>> = mapOf(
        "common" to emptySet(),
        // Preview fixtures build sample data of every kind.
        "preview" to setOf("home"),
        // Crash reports become part of the data module.
        "diagnostics" to setOf("common"),
        "project" to setOf("common", "diagnostics"),
        // Images, sound and video form one media module, so they may use each other.
        "media" to setOf("project", "audio", "video"),
        "audio" to setOf("media"),
        "video" to setOf("media"),
        "actors" to setOf("media"),
        "library" to setOf("actors"),
        "export" to setOf("actors"),
        "studio" to setOf("library", "export", "settings"),
        "settings" to setOf("project"),
        "home" to setOf("studio"),
        "navigation" to setOf("home"),
    )

    /** Each feature with every feature it may use, directly or through another. */
    private val allowed: Map<String, Set<String>> = direct.keys.associateWith { feature ->
        val reached = mutableSetOf<String>()
        val pending = ArrayDeque(direct.getValue(feature))
        while (pending.isNotEmpty()) {
            val next = pending.removeFirst()
            if (reached.add(next)) pending.addAll(direct[next].orEmpty())
        }
        reached
    }

    fun applies(module: String, path: String): Boolean = module == "app" && "/src/main/" in path

    /** The feature a package belongs to, or null for the root package and anything outside the app. */
    fun featureOf(packageName: String): String? =
        packageName.removePrefix("$ROOT.").takeIf { it != packageName }?.substringBefore('.')?.takeIf { it in direct }

    /** The feature [reference] points into when [from] may not use it, otherwise null. */
    fun violation(from: String, reference: String): String? {
        val target = featureOf(reference) ?: return null
        return target.takeIf { it != from && it !in allowed.getValue(from) }
    }
}
