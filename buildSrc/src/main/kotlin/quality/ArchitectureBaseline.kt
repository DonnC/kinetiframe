package quality

import java.util.Properties

internal object ArchitectureBaseline {
    fun allowances(properties: Properties): Map<String, Int> = buildMap {
        properties.stringPropertyNames().forEach { key ->
            val count = properties.getProperty(key).toIntOrNull() ?: return@forEach
            put(key, maxOf(get(key) ?: 0, count))
            val typedParameters = key.substringAfterLast('|').substringAfter('(').any(Char::isLetter)
            if (key.startsWith(ARITY_PREFIX) && typedParameters &&
                (key.contains("(,") || key.contains(",,") || key.endsWith(",)"))
            ) {
                val normalized = key.replace("(,", "(").replace(",,", ",").replace(",)", ")")
                val removedParameters = key.length - normalized.length
                val normalizedCount = count - removedParameters
                if (normalizedCount > 0) put(normalized, maxOf(get(normalized) ?: 0, normalizedCount))
            }
        }
    }

    private const val ARITY_PREFIX = "arity|"
}
