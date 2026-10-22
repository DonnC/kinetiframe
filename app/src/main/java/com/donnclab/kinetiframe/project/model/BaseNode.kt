package com.donnclab.kinetiframe.project.model

import android.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class NodeStyle(
    val mainColorHex: Int = Color.BLACK,
    val alpha: Float = 1f,
    val blurRadius: Float = 0f,
    val glowColorHex: Int? = null,
    val glowRadius: Float = 0f,
    val strokeColorHex: Int = Color.TRANSPARENT,
    val strokeWidthPx: Float = 0f,
)

@Serializable
sealed class BaseNode {
    abstract val id: String
    abstract val x: Float
    abstract val y: Float
    abstract val rotation: Float
    abstract val scaleX: Float
    abstract val scaleY: Float
    abstract val isVisible: Boolean
    abstract val isSelected: Boolean
    abstract val zIndex: Int
    abstract val style: NodeStyle

    // Abstract methods for common operations
    abstract fun copyWithTransform(
        x: Float = this.x,
        y: Float = this.y,
        rotation: Float = this.rotation,
        scaleX: Float = this.scaleX,
        scaleY: Float = this.scaleY,
    ): BaseNode

    abstract fun copyWithSelection(isSelected: Boolean): BaseNode

    abstract fun copyWithId(id: String): BaseNode
}
