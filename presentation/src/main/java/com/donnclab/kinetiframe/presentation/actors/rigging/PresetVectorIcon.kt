package com.donnclab.kinetiframe.presentation.actors.rigging

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Decoder for our bundled single-path 64px icons, not a general external SVG importer. */
object PresetVectorIcon {
    fun decode(source: String): ImageVector {
        require(source.length <= 8192 && source.contains("viewBox=\"0 0 64 64\""))
        val data = requireNotNull(Regex("<path d=\"([^\"]+)\"").find(source)?.groupValues?.get(1))
        return ImageVector.Builder("Motion", 64.dp, 64.dp, 64f, 64f).addPath(
            pathData = PathParser().parsePathString(data).toNodes(),
            fill = null,
            stroke = SolidColor(Color(0xFF172B36)),
            strokeLineWidth = 2.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()
    }
}
