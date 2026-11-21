package com.donnclab.kinetiframe.core.geometry

import kotlinx.serialization.Serializable

/** Six-coefficient, durable stage transform. Unlike TRS, this preserves shear and reflection. */
@Serializable
data class Affine2D(
    val a: Float = 1f,
    val b: Float = 0f,
    val c: Float = 0f,
    val d: Float = 1f,
    val tx: Float = 0f,
    val ty: Float = 0f,
) {
    init {
        require(a.isFinite() && b.isFinite() && c.isFinite() && d.isFinite() && tx.isFinite() && ty.isFinite())
    }

    /** this * other: apply other first, then this (stage-space composition). */
    operator fun times(other: Affine2D) = Affine2D(
        a * other.a + c * other.b,
        b * other.a + d * other.b,
        a * other.c + c * other.d,
        b * other.c + d * other.d,
        a * other.tx + c * other.ty + tx,
        b * other.tx + d * other.ty + ty,
    )

    fun mapX(x: Float, y: Float) = a * x + c * y + tx
    fun mapY(x: Float, y: Float) = b * x + d * y + ty
    fun androidValues() = floatArrayOf(a, c, tx, b, d, ty, 0f, 0f, 1f)
}
