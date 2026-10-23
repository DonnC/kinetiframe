package com.donnclab.kinetiframe.actors.ui.rigging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.actors.render.toMatrix
import com.donnclab.kinetiframe.actors.rig.math.ForwardKinematicsSolver
import com.donnclab.kinetiframe.actors.rig.math.RigJointHitTester
import com.donnclab.kinetiframe.actors.rig.math.RigJointTarget
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.core.document.BrushSnapshot
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.activeActor
import com.donnclab.kinetiframe.studio.actorDefinition
import com.donnclab.kinetiframe.studio.currentFrameLayers
import com.donnclab.kinetiframe.studio.ui.canvas.ActiveStroke
import com.donnclab.kinetiframe.studio.ui.canvas.CanvasInputLayer
import com.donnclab.kinetiframe.studio.ui.canvas.PivotPlacementOverlay
import com.donnclab.kinetiframe.studio.ui.canvas.workspaceGestures

/**
 * The isolated Puppet Studio surface. It shares document input and viewport transforms with FxF,
 * while owning only rig-specific rendering and controls.
 */
@Composable
fun PuppetStudioCanvas(
    state: StudioState,
    onIntent: (StudioIntent) -> Unit,
    rigAssetManager: RigAssetManager?,
    modifier: Modifier = Modifier,
    referenceUnderlay: ((android.graphics.Canvas) -> Unit)? = null,
) {
    val activeStroke = remember { ActiveStroke() }
    val selectionPath = remember { Path() }
    val density = LocalDensity.current.density
    val currentIntent = rememberUpdatedState(onIntent)

    val pressurePaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
    }
    val eraserPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
        }
    }
    SideEffect {
        pressurePaint.color = state.tool.activeConfig.color.toArgb()
        pressurePaint.alpha = (state.tool.activeConfig.opacity * 255).toInt().coerceIn(0, 255)
    }

    val jointTargets =
        remember(
            state.project,
            state.layers,
            state.animation.currentFrame,
            state.actor,
            state.scene.studioMode,
            state.scene.selectedBoneId,
        ) {
            // Resolved through activeActor() rather than re-reading the active layer inline: the part
            // layers this studio creates carry boundBoneId, not puppetInstanceId, so an inline lookup
            // produced an EMPTY joint list the moment a body part was selected -- the pins still drew
            // (they come from actorDefinition) but nothing was draggable, which is why reshaping a
            // limb (longer arms, a different build) silently did nothing.
            val instance = state.activeActor() ?: return@remember emptyList()
            val definition = state.actorDefinition(true) ?: return@remember emptyList()
            val instanceMatrix = instance.transform.toMatrix()
            ForwardKinematicsSolver.resolveWorldStates(definition.sceneGraph).values.flatMap { bone ->
                if (state.actor.takeMoveCharacter &&
                    state.actor.takePhase in listOf(
                        com.donnclab.kinetiframe.studio.PerformanceTakePhase.ARMED,
                        com.donnclab.kinetiframe.studio.PerformanceTakePhase.RECORDING,
                    )
                ) {
                    if (bone.id !in definition.sceneGraph.rootBoneIds) return@flatMap emptyList()
                    val start = floatArrayOf(bone.startPoint.x, bone.startPoint.y)
                    instanceMatrix.mapPoints(start)
                    return@flatMap listOf(
                        RigJointTarget("origin:${bone.id}", androidx.compose.ui.geometry.Offset(start[0], start[1])),
                    )
                }
                if (state.scene.studioMode == com.donnclab.kinetiframe.studio.StudioMode.RIG_SETUP &&
                    state.actor.editTarget == com.donnclab.kinetiframe.studio.ActorEditTarget.JOINT_START
                ) {
                    if (bone.id != state.scene.selectedBoneId) return@flatMap emptyList()
                    val start = floatArrayOf(bone.startPoint.x, bone.startPoint.y)
                    instanceMatrix.mapPoints(start)
                    return@flatMap listOf(
                        RigJointTarget("origin:${bone.id}", androidx.compose.ui.geometry.Offset(start[0], start[1])),
                    )
                }
                val point = floatArrayOf(bone.endPoint.x, bone.endPoint.y)
                instanceMatrix.mapPoints(point)
                // Short attachment anchors (eyes, mouth, labels, accessory pivots) are positions,
                // not useful rotation handles. Drag the visible dot to move their origin in Setup.
                val attachment = state.scene.studioMode == com.donnclab.kinetiframe.studio.StudioMode.RIG_SETUP &&
                    com.donnclab.kinetiframe.actors.rig.math.ActorJointHandle.isAttachment(
                        definition.sceneGraph,
                        bone.id,
                    )
                val endpoint = RigJointTarget(
                    if (attachment) "attachment:${bone.id}" else bone.id,
                    androidx.compose.ui.geometry.Offset(point[0], point[1]),
                )
                if (state.scene.studioMode != com.donnclab.kinetiframe.studio.StudioMode.RASTER_DRAWING &&
                    bone.id in definition.sceneGraph.rootBoneIds
                ) {
                    val start = floatArrayOf(bone.startPoint.x, bone.startPoint.y)
                    instanceMatrix.mapPoints(start)
                    listOf(
                        endpoint,
                        RigJointTarget("origin:${bone.id}", androidx.compose.ui.geometry.Offset(start[0], start[1])),
                    )
                } else {
                    listOf(endpoint)
                }
            }
        }

    // Only explicit bake receipts retire overlays; unrelated frame refreshes do not.
    LaunchedEffect(state.bakedStrokeSeeds) {
        activeStroke.acknowledgeSettledStrokes(state.bakedStrokeSeeds)
    }
    LaunchedEffect(state.animation.currentFrame) {
        activeStroke.reset()
        activeStroke.clearSettledStrokes()
    }
    LaunchedEffect(state.scene.studioMode) {
        activeStroke.reset()
        activeStroke.clearSettledStrokes()
        selectionPath.reset()
    }

    Column(modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .onSizeChanged { onIntent(StudioIntent.View.SetViewportSize(it.width, it.height)) }
                .pointerInput(state.scene.studioMode) {
                    workspaceGestures(
                        touchNavigationEnabled = true,
                        onNavigation = {
                            activeStroke.reset()
                            selectionPath.reset()
                            currentIntent.value(StudioIntent.Actor.CancelDrag)
                        },
                        onTransform = { pan, zoom, rotation, centre ->
                            currentIntent.value(StudioIntent.View.UpdateTransform(pan, zoom, rotation, centre))
                        },
                    )
                },
        ) {
            val config = state.tool.activeConfig
            CanvasInputLayer(
                stage = state.project.stage,
                canvasMatrixValues = state.canvas.matrixValues,
                sourceFrame = state.currentBitmap,
                studioMode = state.scene.studioMode,
                activeTool = state.tool.activeTool,
                pressureSensitive = config.brushId != com.donnclab.kinetiframe.core.document.BrushId.PEN,
                rulerGuide = state.canvas.rulerGuide,
                constructionShape = state.tool.constructionShape,
                constructionPreviewPending = state.constructionShape.previewActive,
                selectionActive = false,
                nodes = emptyMap(),
                displayDensity = density,
                fontRegistry = null,
                selectionTool = state.selection.toolType,
                activeStroke = activeStroke,
                selectionPath = selectionPath,
                onSelectionRectChanged = {},
                onTextNodeSelect = {},
                onSelectionAreaComplete = { path, rect, _, _ ->
                    val definition = state.actorDefinition(true)
                    val instance = state.activeActor()
                    val bounds = rect ?: path?.getBounds()
                    if (definition != null && instance != null && bounds != null && rigAssetManager != null) {
                        val selection = path?.asAndroidPath() ?: android.graphics.Path().apply {
                            addRect(
                                bounds.left,
                                bounds.top,
                                bounds.right,
                                bounds.bottom,
                                android.graphics.Path.Direction.CW,
                            )
                        }
                        val id = com.donnclab.kinetiframe.actors.render.PuppetArtworkHitTester.select(
                            selection,
                            com.donnclab.kinetiframe.actors.render.PuppetRenderPlan.create(
                                definition,
                                instance,
                            ),
                            rigAssetManager,
                        )
                        onIntent(StudioIntent.Rigging.SelectBone(id))
                        if (!state.actor.isPerformanceAuthoring) {
                            onIntent(
                                StudioIntent.Rigging.SetStudioMode(
                                    com.donnclab.kinetiframe.studio.StudioMode.RIG_SETUP,
                                ),
                            )
                            onIntent(
                                StudioIntent.Actor.SetEditTarget(
                                    com.donnclab.kinetiframe.studio.ActorEditTarget.ARTWORK,
                                ),
                            )
                        }
                        onIntent(StudioIntent.Draw.SelectTool(DrawingTool.RIG_SELECT))
                    }
                    selectionPath.reset()
                },
                onEraserCursorChange = {},
                onConstructionPreview = { onIntent(StudioIntent.Construction.SetPreview(it)) },
                onIntent = onIntent,
                onStrokeSettled = { points ->
                    activeStroke.settle(
                        points = points,
                        tool = state.tool.activeTool,
                        eraserType = config.eraserType,
                        width = config.size,
                        strength = config.strength,
                        paint = if (state.tool.activeTool == DrawingTool.ERASER) eraserPaint else pressurePaint,
                        sourceBitmap = state.currentBitmap,
                        brush = BrushSnapshot(config.brushId, seed = points.firstOrNull()?.timestamp ?: 0L),
                    )
                },
                onStrokeComplete = { points ->
                    // The selected part is authoritative. A nearest-bone guess is unsafe for a
                    // continuous contour that passes close to two limbs; the animator intentionally
                    // selects the cut-out boundary first, then sketches that one isolated piece.
                    // The target still travels with this bake so selection cannot race bitmap work.
                    val targetBoneId = if (state.scene.studioMode ==
                        com.donnclab.kinetiframe.studio.StudioMode.RASTER_DRAWING
                    ) {
                        state.scene.selectedBoneId?.takeIf { boneId ->
                            state.actorDefinition()?.sceneGraph?.bones?.containsKey(boneId) == true
                        }
                    } else {
                        null
                    }
                    onIntent(
                        StudioIntent.Draw.BakeStroke(
                            points = points,
                            brush = BrushSnapshot(config.brushId, seed = points.firstOrNull()?.timestamp ?: 0L),
                            targetBoneId = targetBoneId,
                        ),
                    )
                },
                onRigJointHit = { point ->
                    val definition = state.actorDefinition(true)
                    val instance = state.activeActor()
                    if (state.scene.studioMode == com.donnclab.kinetiframe.studio.StudioMode.RIG_SETUP &&
                        state.actor.editTarget == com.donnclab.kinetiframe.studio.ActorEditTarget.ARTWORK &&
                        definition != null && instance != null && rigAssetManager != null
                    ) {
                        com.donnclab.kinetiframe.actors.render.PuppetArtworkHitTester.hit(
                            point,
                            com.donnclab.kinetiframe.actors.render.PuppetRenderPlan.create(
                                definition,
                                instance,
                            ),
                            rigAssetManager,
                        )?.let {
                            "artwork:$it"
                        }
                    } else {
                        RigJointHitTester.nearestWithin(
                            point,
                            jointTargets,
                            radius =
                            12f * density /
                                kotlin.math.hypot(
                                    state.canvas.matrixValues[0],
                                    state.canvas.matrixValues[3],
                                ).coerceAtLeast(0.01f),
                        )
                    }
                },
            ) {
                PuppetSourceLayer(
                    referenceUnderlay = referenceUnderlay,
                    state = state,
                    path = selectionPath,
                    stroke = activeStroke,
                    brush = BrushSnapshot(config.brushId, seed = activeStroke.points.firstOrNull()?.timestamp ?: 0L),
                    baseWidth = config.size,
                    strength = config.strength,
                    drawMarks = false,
                )
                rigAssetManager?.let { assets ->
                    SkeletalRenderLayer(
                        state = state,
                        rigAssetManager = assets,
                        modifier = Modifier.fillMaxSize(),
                        eraseSelectedArtwork = if (state.scene.studioMode ==
                            com.donnclab.kinetiframe.studio.StudioMode.RASTER_DRAWING &&
                            state.tool.activeTool == DrawingTool.ERASER &&
                            config.eraserType == com.donnclab.kinetiframe.studio.EraserType.AREA
                        ) {
                            (
                                { canvas ->
                                    activeStroke.drawTrigger
                                    if (activeStroke.points.isNotEmpty()) {
                                        com.donnclab.kinetiframe.studio.ui.canvas.drawVariableWidthStroke(
                                            canvas,
                                            activeStroke,
                                            config.size,
                                            eraserPaint,
                                            DrawingTool.ERASER,
                                        )
                                    }
                                    activeStroke.settledStrokes.filter {
                                        it.tool == DrawingTool.ERASER &&
                                            it.brush.seed !in state.bakedStrokeSeeds
                                    }
                                        .forEach {
                                            com.donnclab.kinetiframe.studio.ui.canvas.drawVariableWidthStroke(
                                                canvas,
                                                it.points,
                                                baseWidth = it.width,
                                                paint = eraserPaint,
                                                activeTool = DrawingTool.ERASER,
                                            )
                                        }
                                }
                                )
                        } else {
                            null
                        },
                    )
                }
                PuppetSourceLayer(
                    state,
                    selectionPath,
                    activeStroke,
                    BrushSnapshot(config.brushId, seed = activeStroke.points.firstOrNull()?.timestamp ?: 0L),
                    config.size,
                    config.strength,
                    drawSource = false,
                )
                if (state.constructionShape.previewActive) {
                    com.donnclab.kinetiframe.studio.ui.canvas.ConstructionShapePreviewOverlay(
                        strokes = state.constructionShape.previewStrokes,
                        canvasMatrix = android.graphics.Matrix().apply { setValues(state.canvas.matrixValues) },
                        color = config.color,
                        width = config.size,
                        alpha = config.opacity,
                        brushId = config.brushId,
                    )
                }
                if (state.scene.skeletonOverlayVisible) {
                    RigSkeletonOverlay(state = state, modifier = Modifier.fillMaxSize())
                }
            }

            state.scene.pivotPlacement?.let { placement ->
                PivotPlacementOverlay(
                    placement = placement,
                    bitmap = rigAssetManager?.getAsset(placement.assetId),
                    stage = state.project.stage,
                    canvasMatrixValues = state.canvas.matrixValues,
                    onPivotMoved = { x, y -> onIntent(StudioIntent.Rigging.MovePivot(x, y)) },
                    onConfirmed = { onIntent(StudioIntent.Rigging.ConfirmPivot) },
                    onCancelled = { onIntent(StudioIntent.Rigging.CancelPivot) },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (state.scene.studioMode != com.donnclab.kinetiframe.studio.StudioMode.ANIMATE_IK ||
                (
                    state.activeActor()?.poseKeys.isNullOrEmpty() && state.activeActor()?.motionPreset == null &&
                        state.actor.presetPreview == null && !state.actor.previewPlaying
                    )
            ) {
                ActorWorkflowHint(
                    state,
                    Modifier.align(Alignment.TopCenter).padding(start = 144.dp, end = 8.dp, top = 4.dp),
                )
            }
        }
        if (state.scene.studioMode == com.donnclab.kinetiframe.studio.StudioMode.ANIMATE_IK) {
            ActorPoseTimeline(state, onIntent, Modifier.fillMaxWidth(), rigAssetManager)
        }
    }
}

@Preview(showBackground = true, widthDp = 640, heightDp = 360)
@Composable
private fun PuppetStudioCanvasPreview() {
    KinetiFrameTheme {
        PuppetStudioCanvas(
            state = StudioState(),
            onIntent = {},
            rigAssetManager = null,
        )
    }
}
