package com.donnclab.kinetiframe.models

import android.graphics.Color
import android.graphics.Typeface
import com.donnclab.kinetiframe.engine.math.Affine2D
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("com.kinetiframe.models.TextNode")
data class TextNode(
    override val id: String,
    val text: String = "Double tap to edit",
    val fontFileName: String = "inter_regular.ttf",
    val fontSizeSp: Float = 36f,
    val alignment: TextAlignment = TextAlignment.CENTER,
    override val x: Float = 960f,
    override val y: Float = 540f,
    override val rotation: Float = 0f,
    override val scaleX: Float = 1f,
    override val scaleY: Float = 1f,
    override val isSelected: Boolean = false,
    override val isVisible: Boolean = true,
    override val zIndex: Int = 0,
    override val style: NodeStyle = NodeStyle(
        mainColorHex = Color.WHITE,
        strokeColorHex = Color.BLACK,
        strokeWidthPx = 4f,
    ),
    val selectionTransform: Affine2D = Affine2D(),
) : BaseNode() {
    enum class TextAlignment { LEFT, CENTER, RIGHT }

    override fun copyWithTransform(x: Float, y: Float, rotation: Float, scaleX: Float, scaleY: Float): TextNode =
        copy(x = x, y = y, rotation = rotation, scaleX = scaleX, scaleY = scaleY)

    override fun copyWithSelection(isSelected: Boolean): TextNode = copy(isSelected = isSelected)

    override fun copyWithId(id: String): TextNode = copy(id = id)
}

data class FontItem(val displayName: String, val fileName: String, val typeface: Typeface)
