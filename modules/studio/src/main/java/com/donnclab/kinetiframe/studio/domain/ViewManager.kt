package com.donnclab.kinetiframe.studio.domain

import android.graphics.Matrix
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.studio.DockEdge
import com.donnclab.kinetiframe.studio.GridDefaults
import com.donnclab.kinetiframe.studio.GridSize
import com.donnclab.kinetiframe.studio.RulerGuide
import com.donnclab.kinetiframe.studio.RulerMode
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState

/**
 * Manager for grid, ruler, and canvas transformation.
 */
class ViewManager(private val updateState: ((StudioState) -> StudioState) -> Unit) {
    fun setGridSize(size: GridSize) {
        updateState { state ->
            state.copy(canvas = state.canvas.copy(gridSize = size, gridSpacingDp = size.sizeDp.toFloat()))
        }
    }

    fun setGridSpacing(spacingDp: Float) {
        val spacing = spacingDp.coerceIn(0f, 120f)
        updateState { state ->
            state.copy(canvas = state.canvas.copy(gridSpacingDp = spacing))
        }
    }

    fun setGridEnabled(enabled: Boolean) {
        updateState { state ->
            state.copy(canvas = state.canvas.copy(gridEnabled = enabled))
        }
    }

    fun setGridOpacity(opacity: Float) {
        updateState { state ->
            state.copy(canvas = state.canvas.copy(gridOpacity = opacity.coerceIn(0f, 1f)))
        }
    }

    fun setGridSpacingVertical(spacing: Float) {
        updateState { state ->
            state.copy(canvas = state.canvas.copy(gridSpacingVertical = spacing.coerceIn(0f, 120f)))
        }
    }

    fun setGridSpacingHorizontal(spacing: Float) {
        updateState { state ->
            state.copy(canvas = state.canvas.copy(gridSpacingHorizontal = spacing.coerceIn(0f, 120f)))
        }
    }

    fun resetGridSettings() {
        updateState { state ->
            state.copy(
                canvas = state.canvas.copy(
                    gridEnabled = GridDefaults.ENABLED,
                    gridOpacity = GridDefaults.OPACITY,
                    gridSpacingVertical = GridDefaults.SPACING,
                    gridSpacingHorizontal = GridDefaults.SPACING,
                ),
            )
        }
    }

    fun setRulerMode(mode: RulerMode) {
        updateState { state ->
            state.copy(
                canvas = state.canvas.copy(
                    rulerMode = mode,
                    rulerGuide = if (mode == RulerMode.NONE) null else RulerGuide(mode),
                ),
            )
        }
    }

    fun updateRulerGuide(guide: RulerGuide) {
        updateState { state ->
            state.copy(canvas = state.canvas.copy(rulerGuide = guide, rulerMode = guide.mode))
        }
    }

    fun setViewportSize(width: Int, height: Int) {
        updateState { state ->
            val newState = state.copy(canvas = state.canvas.copy(viewportWidth = width, viewportHeight = height))
            updateMatrixInternal(newState)
        }
    }

    fun updateCanvasTransform(matrix: Matrix) {
        updateState { state ->
            val values = FloatArray(9)
            matrix.getValues(values)

            val zoom = extractZoom(values)
            val rotation = extractRotation(values)
            val pan = extractPan(matrix, state.canvas.viewportWidth, state.canvas.viewportHeight, state.project.stage)

            state.copy(
                canvas = state.canvas.copy(
                    matrixValues = values,
                    zoom = zoom,
                    pan = pan,
                    rotation = rotation,
                ),
            )
        }
    }

    fun resetCanvasTransform() {
        updateState { state ->
            val newState = state.copy(
                canvas = state.canvas.copy(
                    zoom = 1f,
                    pan = Point2.Zero,
                    rotation = 0f,
                ),
            )
            updateMatrixInternal(newState)
        }
    }

    /** Fits the canvas when the view has moved; otherwise zooms until the canvas fills the view. */
    fun toggleFillView() {
        updateState { state ->
            val canvas = state.canvas
            val stage = state.project.stage
            val zoom = if (state.isCanvasTransformed || canvas.viewportWidth <= 0 || canvas.viewportHeight <= 0) {
                1f
            } else {
                val across = canvas.viewportWidth.toFloat() / stage.width
                val down = canvas.viewportHeight.toFloat() / stage.height
                // Zoom is relative to the fitted stage, so filling is the larger scale over the smaller.
                maxOf(across, down) / minOf(across, down)
            }
            updateMatrixInternal(state.copy(canvas = canvas.copy(zoom = zoom, pan = Point2.Zero, rotation = 0f)))
        }
    }

    /** Same terms as [updateMatrixInternal]: zoom is relative to the fitted stage, pan in pixels. */
    fun frameStageRect(rect: StudioIntent.View.FrameStageRect) {
        updateState { state ->
            val canvas = state.canvas
            val width = rect.right - rect.left
            val height = rect.bottom - rect.top
            if (canvas.viewportWidth <= 0 || canvas.viewportHeight <= 0 || width <= 0f || height <= 0f) {
                return@updateState state
            }
            val stage = state.project.stage
            val fitScale = minOf(
                canvas.viewportWidth.toFloat() / stage.width,
                canvas.viewportHeight.toFloat() / stage.height,
            )
            // The same zoom limits as a pinch.
            val clear = canvas.viewportHeight * (1f - rect.bottomInset.coerceIn(0f, 0.6f))
            val zoom = (minOf(canvas.viewportWidth / width, clear / height) * rect.fill / fitScale)
                .coerceIn(0.25f, 50f)
            val scale = zoom * fitScale
            val centreX = (rect.left + rect.right) / 2f - stage.width / 2f
            val centreY = (rect.top + rect.bottom) / 2f - stage.height / 2f
            updateMatrixInternal(
                state.copy(
                    canvas = canvas.copy(
                        zoom = zoom,
                        // Centred in the clear part of the canvas, above the bottom panels.
                        pan = Point2(-centreX * scale, -centreY * scale - (canvas.viewportHeight - clear) / 2f),
                        rotation = 0f,
                    ),
                ),
            )
        }
    }

    fun toggleSafeFrame() = updateState { state ->
        state.copy(canvas = state.canvas.copy(showSafeFrame = !state.canvas.showSafeFrame))
    }

    fun updateTransform(panDelta: Point2, zoomDelta: Float, rotationDelta: Float, pivot: Point2) {
        updateState { state ->
            val matrix = Matrix()
            matrix.setValues(state.canvas.matrixValues)

            // CanvasState.zoom is relative to the fitted workspace scale.  Keeping it
            // relative means 100% always fits the document into the space left by the
            // app bar and timeline, regardless of orientation or device size.
            val currentZoom = state.canvas.zoom.coerceAtLeast(0.0001f)
            val targetZoom = (currentZoom * zoomDelta).coerceIn(0.25f, 50f)
            val appliedZoomDelta = targetZoom / currentZoom

            matrix.postTranslate(panDelta.x, panDelta.y)
            matrix.postScale(appliedZoomDelta, appliedZoomDelta, pivot.x, pivot.y)
            matrix.postRotate(rotationDelta, pivot.x, pivot.y)

            val values = FloatArray(9)
            matrix.getValues(values)

            val finalRotation = extractRotation(values)
            // Pan is an accumulated logical offset. Deriving it from a matrix requires
            // a measured viewport and collapses valid gestures to zero before layout.
            val finalPan = state.canvas.pan + panDelta

            state.copy(
                canvas = state.canvas.copy(
                    matrixValues = values,
                    zoom = targetZoom,
                    pan = finalPan,
                    rotation = finalRotation,
                ),
            )
        }
    }

    private fun extractZoom(values: FloatArray): Float {
        val scaleX = values[Matrix.MSCALE_X]
        val skewY = values[Matrix.MSKEW_Y]
        return kotlin.math.sqrt((scaleX * scaleX + skewY * skewY).toDouble()).toFloat()
    }

    private fun extractRotation(values: FloatArray): Float {
        val scaleX = values[Matrix.MSCALE_X]
        val skewY = values[Matrix.MSKEW_Y]
        return Math.toDegrees(Math.atan2(skewY.toDouble(), scaleX.toDouble())).toFloat()
    }

    private fun extractPan(matrix: Matrix, viewportWidth: Int, viewportHeight: Int, stage: StageDimensions): Point2 {
        if (viewportWidth <= 0 || viewportHeight <= 0) return Point2.Zero
        val pts = floatArrayOf(stage.width / 2f, stage.height / 2f)
        matrix.mapPoints(pts)
        return Point2(pts[0] - viewportWidth / 2f, pts[1] - viewportHeight / 2f)
    }

    private fun updateMatrixInternal(state: StudioState): StudioState {
        val canvas = state.canvas
        if (canvas.viewportWidth <= 0 || canvas.viewportHeight <= 0) return state

        val matrix = Matrix()
        // Matrix = Translate(pan) * Translate(VC) * Scale(zoom) * Rotate(rotation) * Translate(-DC)

        val stage = state.project.stage
        val docCenterX = stage.width / 2f
        val docCenterY = stage.height / 2f
        val viewportCenterX = canvas.viewportWidth / 2f
        val viewportCenterY = canvas.viewportHeight / 2f
        val fitScale = minOf(
            canvas.viewportWidth.toFloat() / stage.width,
            canvas.viewportHeight.toFloat() / stage.height,
        )
        val renderedScale = fitScale * canvas.zoom

        matrix.setTranslate(-docCenterX, -docCenterY)
        matrix.postScale(renderedScale, renderedScale)
        matrix.postRotate(canvas.rotation)
        matrix.postTranslate(viewportCenterX + canvas.pan.x, viewportCenterY + canvas.pan.y)

        val values = FloatArray(9)
        matrix.getValues(values)

        return state.copy(
            canvas = state.canvas.copy(matrixValues = values),
        )
    }

    fun setDockPosition(edge: DockEdge, offset: Point2) {
        updateState { state ->
            state.copy(canvas = state.canvas.copy(dockEdge = edge, dockOffset = offset))
        }
    }

    fun handle(intent: StudioIntent.View) {
        when (intent) {
            is StudioIntent.View.SetGridSize -> setGridSize(intent.size)

            is StudioIntent.View.SetGridSpacing -> setGridSpacing(intent.spacingDp)

            is StudioIntent.View.SetGridEnabled -> setGridEnabled(intent.enabled)

            is StudioIntent.View.SetGridOpacity -> setGridOpacity(intent.opacity)

            is StudioIntent.View.SetGridSpacingVertical -> setGridSpacingVertical(intent.spacing)

            is StudioIntent.View.SetGridSpacingHorizontal -> setGridSpacingHorizontal(intent.spacing)

            StudioIntent.View.ResetGridSettings -> resetGridSettings()

            is StudioIntent.View.SetRulerMode -> setRulerMode(intent.mode)

            is StudioIntent.View.UpdateRulerGuide -> updateRulerGuide(intent.guide)

            is StudioIntent.View.UpdateCanvasTransform -> updateCanvasTransform(intent.matrix)

            StudioIntent.View.ResetCanvasTransform -> resetCanvasTransform()

            StudioIntent.View.ToggleFillView -> toggleFillView()

            is StudioIntent.View.FrameStageRect -> frameStageRect(intent)

            StudioIntent.View.ToggleSafeFrame -> toggleSafeFrame()

            is StudioIntent.View.SetViewportSize -> setViewportSize(intent.width, intent.height)

            is StudioIntent.View.UpdateTransform -> updateTransform(
                intent.panDelta,
                intent.zoomDelta,
                intent.rotationDelta,
                intent.pivot,
            )

            is StudioIntent.View.SetDockPosition -> setDockPosition(intent.edge, intent.offset)
        }
    }
}
