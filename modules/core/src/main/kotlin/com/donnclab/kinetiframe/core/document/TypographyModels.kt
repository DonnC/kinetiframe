package com.donnclab.kinetiframe.core.document

import com.donnclab.kinetiframe.core.geometry.Affine2D
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Opaque white as packed ARGB, the default fill of a text node. */
const val ARGB_WHITE: Int = 0xFFFFFFFF.toInt()

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
        mainColorHex = ARGB_WHITE,
        strokeColorHex = ARGB_BLACK,
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
