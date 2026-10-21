// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.sandbox.rigging

import com.donnclab.kinetiframe.core.actor.artwork.ArtworkSkin
import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.artwork.SkinBone
import com.donnclab.kinetiframe.core.actor.artwork.SkinGrid
import com.donnclab.kinetiframe.core.actor.artwork.SkinMesh
import com.donnclab.kinetiframe.core.actor.artwork.SkinSourceSize
import com.donnclab.kinetiframe.core.actor.artwork.SkinVertex
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.math.SkinDeformation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal object RigMeshFixture {
    const val COLUMNS = 8
    const val ROWS = 2
    private const val THICKNESS = 20f
    private const val UPPER_ID = "upper"
    private const val LOWER_ID = "lower"

    private val skin = run {
        val joints = RigFixture.joints
        val world = world(joints)
        val vertices = (0..ROWS).flatMap { row ->
            (0..COLUMNS).map { column ->
                val t = column.toFloat() / COLUMNS
                val start = if (t <= .5f) joints[0] else joints[1]
                val end = if (t <= .5f) joints[1] else joints[2]
                val axis = end - start
                val fraction = if (t <= .5f) t * 2 else (t - .5f) * 2
                val normal = Point2(-axis.y, axis.x) / axis.getDistance()
                val point = start + axis * fraction + normal * ((row.toFloat() / ROWS - .5f) * THICKNESS)
                val lowerWeight = t * t * (3 - 2 * t)
                SkinVertex(point.x, point.y, listOf(1 - lowerWeight, lowerWeight))
            }
        }
        ArtworkSkin(
            SkinSourceSize(RigFixture.WIDTH.toInt(), RigFixture.HEIGHT.toInt()),
            SkinGrid(COLUMNS, ROWS),
            SkinMesh(
                listOf(SkinBone(UPPER_ID, world.getValue(UPPER_ID)), SkinBone(LOWER_ID, world.getValue(LOWER_ID))),
                vertices,
            ),
        )
    }

    fun deform(joints: List<Point2>): FloatArray = SkinDeformation.deform(skin, world(joints))

    private fun world(joints: List<Point2>) =
        mapOf(UPPER_ID to matrix(joints[0], joints[1]), LOWER_ID to matrix(joints[1], joints[2]))

    private fun matrix(start: Point2, end: Point2): SkinAffine {
        val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
        return SkinAffine(
            Point2(cos(angle).toFloat(), -sin(angle).toFloat()),
            Point2(sin(angle).toFloat(), cos(angle).toFloat()),
            start,
        )
    }
}
