package com.donnclab.kinetiframe.studio.ui.canvas

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.studio.AnimationState
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.actorDefinition
import com.donnclab.kinetiframe.studio.displayFrame
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import com.donnclab.kinetiframe.studio.ui.actorstudio.rigging.ActorWorkflowHint
import com.donnclab.kinetiframe.studio.ui.actorstudio.rigging.ProjectActorRenderLayer
import com.donnclab.kinetiframe.studio.ui.actorstudio.rigging.PuppetStudioCanvas
import com.donnclab.kinetiframe.studio.ui.actorstudio.rigging.RigSkeletonOverlay

@Composable
fun CanvasArea(
    cameraPreview: Boolean = false,
    cameraPoseOverride: com.donnclab.kinetiframe.core.document.camera.CameraPose? = null,
    fitStage: Boolean = false,
    actorReference: com.donnclab.kinetiframe.studio.ui.actorstudio.ActorReferenceSession? = null,
    state: StudioState,
    onIntent: (StudioIntent) -> Unit,
    modifier: Modifier = Modifier,
    cacheManager: FrameCacheManager? = null,
    rigAssetManager: RigAssetManager? = null,
    actorFiles: DocumentFileStore? = null,
    actorRepository: ActorRepository? = null,
    performanceRepository: PerformanceRepository? = null,
    projectAssetManager: com.donnclab.kinetiframe.studio.media.ProjectAssetManager? = null,
    videoFrameProvider: com.donnclab.kinetiframe.studio.media.VideoFrameProvider? = null,
) {
    val enabled = cameraPreview && (state.project.cameraShots.isNotEmpty() || cameraPoseOverride != null)
    val raster = com.donnclab.kinetiframe.studio.ui.camera.cameraRasterFrame(
        state,
        cacheManager,
        enabled && fitStage,
    )
    val followed = com.donnclab.kinetiframe.studio.ui.camera.followCameraPose(
        state.project,
        state.animation.displayFrame(),
        actorRepository,
        performanceRepository,
        actorFiles,
        enabled && cameraPoseOverride == null,
    )
    val pose = cameraPoseOverride ?: followed ?: com.donnclab.kinetiframe.engine.camera.SceneCamera.evaluate(
        state.project.cameraShots,
        state.animation.displayFrame(),
        state.project.fps,
    )
    val stage = state.project.stage
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val values = remember(fitStage, viewportSize, stage, state.canvas.matrixValues) {
        if (!fitStage || viewportSize.width == 0 || viewportSize.height == 0) {
            state.canvas.matrixValues
        } else {
            val scale =
                minOf(viewportSize.width.toFloat() / stage.width, viewportSize.height.toFloat() / stage.height) * .92f
            com.donnclab.kinetiframe.core.geometry.Affine2D(
                a = scale,
                d = scale,
                tx = (viewportSize.width - stage.width * scale) / 2f,
                ty = (viewportSize.height - stage.height * scale) / 2f,
            ).androidValues()
        }
    }
    val framing = remember(enabled, pose, values, stage) {
        if (!enabled) {
            values
        } else {
            (
                com.donnclab.kinetiframe.core.geometry.Affine2D(
                    values[0],
                    values[3],
                    values[1],
                    values[4],
                    values[2],
                    values[5],
                ) *
                    com.donnclab.kinetiframe.engine.camera.SceneCamera.matrix(pose, stage.width, stage.height)
                ).androidValues()
        }
    }
    val shown = if (!enabled) {
        state
    } else {
        state.copy(
            currentBitmap = raster ?: state.currentBitmap,
            canvas = state.canvas.copy(
                matrixValues = framing,
                gridEnabled = false,
                backgroundVisible = true,
                rulerGuide = null,
            ),
            selection = state.selection.copy(active = false),
            onionSkin = state.onionSkin.copy(enabled = false),
            tool = state.tool.copy(eraserCursor = null),
            constructionShape = state.constructionShape.copy(previewActive = false),
            fill = state.fill.copy(previewActive = false),
            scene = state.scene.copy(skeletonOverlayVisible = false),
        )
    }
    Box(
        modifier.fillMaxSize().onSizeChanged {
            viewportSize = it
            // Preview suppresses editing intents, but the real workspace still needs layout.
            // Never publish the camera dialog's fitted preview size into the project viewport.
            if (!fitStage && it.width > 0 && it.height > 0) {
                onIntent(StudioIntent.View.SetViewportSize(it.width, it.height))
            }
        }.background(MaterialTheme.colorScheme.surface),
    ) {
        CanvasAreaContents(
            actorReference, shown, { intent ->
                if (com.donnclab.kinetiframe.studio.ui.camera.CameraWorkspacePolicy.acceptsIntent(
                        enabled,
                        fitStage,
                        intent,
                    )
                ) {
                    onIntent(intent)
                }
            },
            Modifier.fillMaxSize().sceneCameraViewport(enabled, values, stage.width, stage.height, pose.black),
            cacheManager, rigAssetManager, actorFiles, actorRepository, performanceRepository,
            projectAssetManager, videoFrameProvider, enabled,
        )
        // Preview cannot silently paint in camera coordinates. Use the explicit pencil toggle
        // to return to untransformed artwork editing; playback/scrubbing still show the camera.
        if (enabled) {
            com.donnclab.kinetiframe.studio.ui.camera.CameraPreviewInputLayer(
                navigationEnabled = !fitStage,
                onIntent = onIntent,
            )
        }
    }
}

@Composable
private fun CanvasAreaContents(
    actorReference: com.donnclab.kinetiframe.studio.ui.actorstudio.ActorReferenceSession? = null,
    state: StudioState,
    onIntent: (StudioIntent) -> Unit,
    modifier: Modifier = Modifier,
    cacheManager: FrameCacheManager? = null,
    rigAssetManager: RigAssetManager? = null,
    actorFiles: DocumentFileStore? = null,
    actorRepository: ActorRepository? = null,
    performanceRepository: PerformanceRepository? = null,
    /** Owns the decoded bitmap for the project's persistent background image, if one is set
     *  (managed by [com.donnclab.kinetiframe.studio.domain.BackgroundManager]). */
    projectAssetManager: com.donnclab.kinetiframe.studio.media.ProjectAssetManager? = null,
    /** Bounded asynchronous decoder shared by embedded scene video and legacy tracing clips. */
    videoFrameProvider: com.donnclab.kinetiframe.studio.media.VideoFrameProvider? = null,
    cameraPreview: Boolean = false,
) {
    // Do not use currentFrame here: PlaybackManager deliberately keeps it stable so the editor's
    // selected frame/layer never jumps while the audience sees playbackFrame advancing.
    val displayFrame = state.animation.displayFrame()
    var actorGeometry by remember(state.project.id) {
        mutableStateOf<com.donnclab.kinetiframe.actors.render.ActorSelectionGeometry?>(null)
    }
    val selectionTransform = remember(state.selection.selectedContent) {
        SelectionTransformState(state.selection.transform, state.selection.pivotOffset)
    }
    androidx.compose.runtime.LaunchedEffect(state.selection.transform, state.selection.pivotOffset) {
        selectionTransform.matrix = state.selection.transform
        selectionTransform.pivotOffset = state.selection.pivotOffset
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val fonts = remember(context) {
        com.donnclab.kinetiframe.studio.drawing.FontRegistry(context)
    }
    val assetRevision = projectAssetManager?.revision?.collectAsStateWithLifecycle()?.value
    val backgroundBitmap = remember(projectAssetManager, assetRevision, state.project.backgroundAssetId) {
        projectAssetManager?.getAsset(state.project.backgroundAssetId)
    }
    val activeVideo = remember(displayFrame, state.project.videoClips) {
        state.project.videoClips.values.firstOrNull { displayFrame in it.timelineStartFrame until it.timelineEndFrame }
    }
    androidx.compose.runtime.LaunchedEffect(
        videoFrameProvider,
        displayFrame,
        state.project.videoClips,
        state.project.assets,
    ) {
        videoFrameProvider?.request(displayFrame, state.project)
    }
    val decodedVideo = videoFrameProvider?.frames?.collectAsStateWithLifecycle()?.value
    // During playback display the latest completed frame from THIS clip. During paused scrubs
    // require the exact frame. Never flash imagery from another clip/project/source.
    val videoFrameBitmap = decodedVideo?.takeIf {
        it.projectId == state.project.id &&
            it.clipId == activeVideo?.clipId &&
            it.sourcePath == activeVideo?.let { clip -> state.project.assets[clip.assetId]?.sourcePath } &&
            it.timelineStartFrame == activeVideo?.timelineStartFrame &&
            it.sourceInSeconds == activeVideo?.videoSourceOffsetSeconds &&
            (if (state.animation.isPlaying) it.frame <= displayFrame else it.frame == displayFrame)
    }?.bitmap
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (cameraPreview) Color.Black else MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        val gridThemeColor = MaterialTheme.colorScheme.tertiary
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        Canvas(
            // The sheet background is rendered by BufferedDrawingCanvas.  Give the
            // grid a higher drawing order so it remains visible on that opaque white
            // surface without participating in pointer input.
            modifier = Modifier.fillMaxSize().zIndex(1f),
        ) {
            drawIntoCanvas { canvas ->
                val v = state.canvas.matrixValues
                val androidMatrix = android.graphics.Matrix()
                androidMatrix.setValues(v)
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.concat(androidMatrix)
                // The grid belongs to the document, not the editor viewport.  Without
                // this clip it can be drawn into the letterbox area around a rotated
                // or zoomed-out sheet.
                canvas.nativeCanvas.clipRect(
                    0f,
                    0f,
                    state.project.stage.width.toFloat(),
                    state.project.stage.height.toFloat(),
                )

                if (state.canvas.gridEnabled) {
                    val gridColor = gridThemeColor.copy(alpha = state.canvas.gridOpacity)
                    val vSpacing = state.canvas.gridSpacingVertical
                    val hSpacing = state.canvas.gridSpacingHorizontal

                    val docWidth = state.project.stage.width.toFloat()
                    val docHeight = state.project.stage.height.toFloat()

                    val paint = android.graphics.Paint().apply {
                        color = gridColor.toArgb()
                        // Ensure the grid stroke width is at least 1dp on screen.
                        // In document space, 1dp is (density / zoom).
                        strokeWidth = (density / state.canvas.zoom).coerceAtLeast(0.5f / state.canvas.zoom)
                        style = android.graphics.Paint.Style.STROKE
                    }

                    if (vSpacing > 0f) {
                        var x = 0f
                        while (x <= docWidth) {
                            canvas.nativeCanvas.drawLine(x, 0f, x, docHeight, paint)
                            x += vSpacing
                        }
                    }
                    if (hSpacing > 0f) {
                        var y = 0f
                        while (y <= docHeight) {
                            canvas.nativeCanvas.drawLine(0f, y, docWidth, y, paint)
                            y += hSpacing
                        }
                    }
                }
                canvas.nativeCanvas.restore()
            }
        }

        // Standalone actor drawing needs the same whole-rig context as setup/posing. The
        // project raster canvas only knows its active bitmap and project placements; routing an
        // actor there made all the other bound parts vanish when switching to the pencil.
        val actorDrawing =
            actorReference != null && state.actorDefinition() != null && !state.actor.isPerformanceAuthoring
        if (state.scene.studioMode == StudioMode.RASTER_DRAWING && !actorDrawing) {
            RasterWorkspaceCanvas(
                state = state,
                onIntent = onIntent,
                cacheManager = cacheManager,
                backgroundBitmap = backgroundBitmap.takeIf { state.canvas.backgroundVisible },
                videoFrameBitmap = videoFrameBitmap.takeIf { !cameraPreview && activeVideo?.isSceneContent == false },
                videoOpacity = state.project.videoTrackOpacity,
                actorGeometry = actorGeometry,
                sharedFontRegistry = fonts,
                externalSelectionOverlay = true,
                paperDecoration = !cameraPreview,
                referenceUnderlay = if (cameraPreview) {
                    null
                } else {
                    actorReference?.let { reference ->
                        { canvas -> reference.draw(canvas) }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (activeVideo?.isSceneContent == true && videoFrameBitmap != null) {
                SceneVideoCanvas(
                    activeVideo,
                    videoFrameBitmap,
                    state.canvas.matrixValues,
                    state.project.stage.width,
                    state.project.stage.height,
                    preview = {
                        if (state.selection.active &&
                            activeVideo.clipId in state.selection.selectedContent?.videos.orEmpty()
                        ) {
                            selectionTransform.matrix.values.let {
                                com.donnclab.kinetiframe.core.geometry.Affine2D(
                                    it[0],
                                    it[1],
                                    it[4],
                                    it[5],
                                    it[12],
                                    it[13],
                                )
                            }
                        } else {
                            com.donnclab.kinetiframe.core.geometry.Affine2D()
                        }
                    },
                    modifier = Modifier.fillMaxSize().zIndex(1.5f),
                    clipToStage = !cameraPreview,
                )
            }
            if (actorFiles != null && actorRepository != null && performanceRepository != null) {
                ProjectActorRenderLayer(
                    placements = state.project.actorPlacements.values,
                    projectId = state.project.id,
                    frame = displayFrame,
                    fps = state.project.fps,
                    canvasMatrix = state.canvas.matrixValues,
                    actors = actorRepository,
                    performances = performanceRepository,
                    files = actorFiles,
                    onGeometryResolved = { actorGeometry = it },
                    selectedIds = state.selection.selectedContent?.actors?.keys.orEmpty(),
                    selectionPreview = if (state.selection.active) ({ selectionTransform.matrix }) else null,
                    modifier = Modifier.fillMaxSize().zIndex(2f),
                )
            }
            if (state.scene.skeletonOverlayVisible && state.actorDefinition() != null) {
                RigSkeletonOverlay(state = state, modifier = Modifier.fillMaxSize().zIndex(2.5f))
            }
            if (state.actorDefinition() != null) {
                ActorWorkflowHint(
                    state,
                    Modifier.align(Alignment.TopCenter)
                        .padding(start = 64.dp, end = 8.dp, top = 4.dp).zIndex(4f),
                )
            }
        } else {
            PuppetStudioCanvas(
                referenceUnderlay = if (state.scene.studioMode !=
                    StudioMode.ANIMATE_IK
                ) {
                    actorReference?.let { reference -> { canvas -> reference.draw(canvas) } }
                } else {
                    null
                },
                state = state,
                onIntent = onIntent,
                rigAssetManager = rigAssetManager,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (!cameraPreview) StageGuideOverlay(state = state, modifier = Modifier.fillMaxSize().zIndex(3f))
        if (actorReference != null && actorReference.editing && actorReference.bitmap != null &&
            state.scene.studioMode != StudioMode.ANIMATE_IK
        ) {
            SelectionTransformBox(
                selectedContent = null, selectionBounds = actorReference.bounds,
                transformState = actorReference.transform, fontRegistry = fonts, displayDensity = density,
                canvasMatrixValues = state.canvas.matrixValues,
                docWidth = state.project.stage.width, docHeight = state.project.stage.height,
                viewportWidth = 0, viewportHeight = 0,
                onOutsideSelectionTap = { actorReference.editing = false },
                onIntent = {}, activeStroke = null, lockAspectRatio = actorReference.lockAspectRatio,
                skewMode = actorReference.skewMode,
                modifier = Modifier.fillMaxSize().zIndex(6f),
            )
        }
        if (state.selection.active &&
            state.scene.studioMode == StudioMode.RASTER_DRAWING
        ) {
            state.selection.bounds?.let { bounds ->
                SelectionTransformBox(
                    selectedContent = state.selection.selectedContent,
                    selectionBounds = bounds,
                    transformState = selectionTransform,
                    fontRegistry = fonts,
                    displayDensity = density,
                    canvasMatrixValues = state.canvas.matrixValues,
                    docWidth = state.project.stage.width, docHeight = state.project.stage.height,
                    viewportWidth = 0, viewportHeight = 0,
                    onOutsideSelectionTap = { onIntent(StudioIntent.Selection.SetActive(false)) },
                    onIntent = onIntent, activeStroke = null,
                    lockAspectRatio = state.selection.lockAspectRatio,
                    skewMode = state.selection.skewMode,
                    interactionMode = state.selection.interactionMode,
                    onDragEnd = {
                        onIntent(StudioIntent.Selection.UpdateTransform(selectionTransform.matrix))
                        onIntent(StudioIntent.Selection.UpdatePivot(selectionTransform.pivotOffset))
                    },
                    modifier = Modifier.fillMaxSize().zIndex(5f),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CanvasAreaPreview() {
    KinetiFrameTheme {
        CanvasArea(
            state = StudioState(animation = AnimationState(currentFrame = 0)),
            onIntent = {},
        )
    }
}
