package com.donnclab.kinetiframe.studio.drawing

import android.content.Context
import android.graphics.Typeface
import com.donnclab.kinetiframe.project.model.FontItem
import java.util.Locale

class FontRegistry(private val context: Context) {

    val availableFonts = mutableListOf<FontItem>()

    init {
        loadFontsFromAssets()
    }

    private fun loadFontsFromAssets() {
        try {
            val fontFiles = context.assets.list("fonts") ?: return

            for (fileName in fontFiles) {
                if (fileName.endsWith(".ttf") || fileName.endsWith(".otf")) {
                    val typeface = Typeface.createFromAsset(context.assets, "fonts/$fileName")
                    val displayName = formatDisplayName(fileName)

                    availableFonts.add(FontItem(displayName, fileName, typeface))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Add default system fonts if assets are empty
        if (availableFonts.isEmpty()) {
            availableFonts.add(FontItem("Inter Regular", "inter_regular.ttf", Typeface.DEFAULT))
            availableFonts.add(FontItem("Bangers Regular", "bangers_regular.ttf", Typeface.DEFAULT_BOLD))
        }
    }

    private fun formatDisplayName(fileName: String): String = fileName
        .substringBeforeLast(".")
        .replace("_", " ")
        .split(" ")
        .joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
}
