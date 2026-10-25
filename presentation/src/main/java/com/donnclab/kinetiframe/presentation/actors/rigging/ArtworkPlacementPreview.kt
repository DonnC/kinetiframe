package com.donnclab.kinetiframe.presentation.actors.rigging

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import com.donnclab.kinetiframe.actors.render.PuppetArtworkRenderer
import com.donnclab.kinetiframe.actors.render.PuppetRenderCommand
import com.donnclab.kinetiframe.actors.render.PuppetRenderPlan
import com.donnclab.kinetiframe.actors.rig.math.ArtworkBindingTransform
import com.donnclab.kinetiframe.actors.rig.math.RigGeometry
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.core.document.PuppetInstance
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.designsystem.theme.BrandTeal
import com.donnclab.kinetiframe.engine.rigging.math.ArtworkGestureBasis
import com.donnclab.kinetiframe.engine.rigging.math.ArtworkGestureDelta
import com.donnclab.kinetiframe.engine.rigging.math.ArtworkPlacementGesture

/** Fixed actor framing: dragging artwork must not make the viewport chase it. Pixels are borrowed. */
@Composable
fun ArtworkPlacementPreview(
    bitmap: Bitmap,
    binding: TextureBinding,
    graph: SceneGraph?,
    partId: String?,
    assets: RigAssetManager?,
    worldAngle: Float,
    onChange: (TextureBinding) -> Unit,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val latest by rememberUpdatedState(binding)
    val changed by rememberUpdatedState(onChange)
    val world = remember(graph) { graph?.let(RigGeometry::resolve) }
    val origin = world?.get(partId)?.start ?: Offset.Zero
    val framing = remember(graph, size) {
        val points = world?.values?.flatMap { listOf(it.start, it.end) }.orEmpty()
        val left = points.minOfOrNull { it.x } ?: -150f
        val top = points.minOfOrNull { it.y } ?: -150f
        val right = points.maxOfOrNull { it.x } ?: 150f
        val bottom = points.maxOfOrNull { it.y } ?: 150f
        val fit = minOf(size.width / (right - left + 100f), size.height / (bottom - top + 100f)).coerceAtLeast(.001f)
        Triple(fit, size.width / 2f - (left + right) / 2f * fit, size.height / 2f - (top + bottom) / 2f * fit)
    }
    val backdrop = remember(graph, partId) {
        graph?.let {
            PuppetRenderPlan.create(
                PuppetDefinition("placement", "Placement", it),
                PuppetInstance("preview", "placement"),
            )
        }
            .orEmpty().filter { it.boneId != partId }
    }
    val command = remember(binding, graph, partId, origin, worldAngle) {
        val node = graph?.bones?.get(partId)
        val previous = node?.textureBinding?.takeIf { it.assetId == binding.assetId }
            ?: node?.artworkVariants?.values?.firstOrNull { it.binding.assetId == binding.assetId }?.binding
        if (graph != null && node != null && previous?.skin != null) {
            val adjusted = ArtworkBindingTransform.apply(
                graph,
                node.id,
                previous,
                binding,
            )
            val audition = graph.copy(bones = graph.bones + (node.id to node.withArtwork(textureBinding = adjusted)))
            PuppetRenderPlan.create(
                PuppetDefinition("placement", "Placement", audition),
                PuppetInstance("preview", "placement"),
            ).firstOrNull { it.boneId == node.id }
                ?.let { return@remember it }
        }
        PuppetRenderCommand(
            "placement",
            Matrix().apply {
                postTranslate(-binding.pivotX, -binding.pivotY)
                postScale(binding.scale, binding.scale)
                postRotate(binding.rotationDegrees)
                postTranslate(binding.offsetX, binding.offsetY)
                postRotate(worldAngle)
                postTranslate(origin.x, origin.y)
            },
        )
    }
    val imagePaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val ghostPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { alpha = 95 } }
    Canvas(
        modifier.semantics {
            contentDescription = "Artwork placement preview"
        }.onSizeChanged { size = it }.pointerInput(framing, worldAngle) {
            detectTransformGestures { _, pan, zoom, rotation ->
                changed(
                    ArtworkPlacementGesture.apply(
                        latest,
                        ArtworkGestureDelta(
                            Point2(pan.x, pan.y),
                            zoom,
                            rotation,
                        ),
                        ArtworkGestureBasis(framing.first, worldAngle),
                    ),
                )
            }
        },
    ) {
        drawRect(Color.White)
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val save = native.save()
            native.translate(framing.second, framing.third)
            native.scale(framing.first, framing.first)
            backdrop.forEach { item ->
                assets?.getAsset(item.assetId)?.let { PuppetArtworkRenderer.draw(native, item, it, ghostPaint) }
            }
            PuppetArtworkRenderer.draw(native, command, bitmap, imagePaint)
            native.restoreToCount(save)
        }
        world?.values?.forEach {
            fun screen(p: Offset) = p * framing.first + Offset(framing.second, framing.third)
            drawLine(BrandTeal.copy(alpha = .35f), screen(it.start), screen(it.end), 2f)
        }
        drawCircle(BrandTeal, 5f, origin * framing.first + Offset(framing.second, framing.third))
    }
}

@Preview(widthDp = 320, heightDp = 240)
@Composable
private fun ArtworkPlacementPreviewSample() {
    val bitmap =
        remember {
            Bitmap.createBitmap(32, 48, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.CYAN) }
        }
    DisposableEffect(bitmap) { onDispose { bitmap.recycle() } }
    ArtworkPlacementPreview(bitmap, TextureBinding("preview", 16f, 24f), null, null, null, 0f, {})
}
