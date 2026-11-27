package com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging

import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.donnclab.kinetiframe.actors.render.PuppetArtworkRenderer
import com.donnclab.kinetiframe.actors.render.PuppetRenderPlan
import com.donnclab.kinetiframe.actors.render.toMatrix
import com.donnclab.kinetiframe.actors.rig.math.ForwardKinematicsSolver
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.core.document.PuppetInstance
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.activeActor
import com.donnclab.kinetiframe.studio.actorDefinition

/**
 * Frames the actor large when Actor Studio opens it, so you see the character rather than a
 * small figure in a big empty stage. It runs again when the mode or the canvas size changes, since
 * each mode lays the canvas out differently; pinching in between is yours.
 */
@Composable
internal fun FrameActorOnOpen(state: StudioState, onIntent: (StudioIntent) -> Unit) {
    val definition = state.actorDefinition(true)
    val instance = state.activeActor()
    val width = state.canvas.viewportWidth
    val height = state.canvas.viewportHeight
    val mode = state.scene.studioMode
    val measured = width > 0 && height > 0
    var framed by remember(definition?.id, mode, width, height) { mutableStateOf(false) }
    LaunchedEffect(definition?.id, instance?.id, mode, width, height) {
        if (framed || !measured || definition == null || instance == null) return@LaunchedEffect
        ActorFraming.bounds(definition, instance)?.let { bounds ->
            onIntent(
                StudioIntent.View.FrameStageRect(
                    bounds.left,
                    bounds.top,
                    bounds.right,
                    bounds.bottom,
                    ActorStudioTokens.ACTOR_FRAME_FILL,
                    // Animate's timeline sits outside the canvas; other modes put the part rail over it.
                    if (mode == StudioMode.ANIMATE_IK) 0f else ActorStudioTokens.ACTOR_FRAME_BOTTOM_INSET,
                ),
            )
        }
        framed = true
    }
}

internal object ActorFraming {
    /** The actor's extent on the stage: its drawn figure if it has one, else its bones. */
    fun bounds(definition: PuppetDefinition, instance: PuppetInstance): RectF? {
        val result = RectF()
        var any = false
        fun add(x: Float, y: Float) {
            if (!any) result.set(x, y, x, y) else result.union(x, y)
            any = true
        }
        val matrix = instance.transform.toMatrix()
        ForwardKinematicsSolver.resolveWorldStates(definition.sceneGraph).values.forEach { bone ->
            val points = floatArrayOf(bone.startPoint.x, bone.startPoint.y, bone.endPoint.x, bone.endPoint.y)
            matrix.mapPoints(points)
            add(points[0], points[1])
            add(points[2], points[3])
        }
        if (definition.sceneGraph.figure != null) {
            PuppetRenderPlan.create(definition, instance).firstOrNull { it.figure != null }?.let { command ->
                PuppetArtworkRenderer.outline(command) { null }?.toList()?.chunked(2)?.forEach { add(it[0], it[1]) }
            }
        }
        if (!any) return null
        // Room around the art for cutouts beyond their bones and the handle rings at the tips.
        val margin = maxOf(result.width(), result.height()) * MARGIN
        result.inset(-margin, -margin)
        return result
    }

    private const val MARGIN = 0.12f
}
