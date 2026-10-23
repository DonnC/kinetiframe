package com.donnclab.kinetiframe.studio

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import com.donnclab.kinetiframe.core.document.DrawingTool

// Pure state changes for studio intents that need no manager, file or bitmap work. Each returns
// the next state, so StudioViewModel applies them inside one updateState call.

internal fun StudioState.reduce(intent: StudioIntent.OnionSkin): StudioState = when (intent) {
    is StudioIntent.OnionSkin.SetEnabled -> copy(onionSkin = onionSkin.copy(enabled = intent.enabled))

    is StudioIntent.OnionSkin.SetFramesBefore -> copy(
        onionSkin = onionSkin.copy(depthBefore = intent.frames.coerceIn(0, 4)),
    )

    is StudioIntent.OnionSkin.SetFramesAfter -> copy(
        onionSkin = onionSkin.copy(depthAfter = intent.frames.coerceIn(0, 4)),
    )

    is StudioIntent.OnionSkin.SetColorBefore -> copy(onionSkin = onionSkin.copy(colorBefore = intent.color))

    is StudioIntent.OnionSkin.SetColorAfter -> copy(onionSkin = onionSkin.copy(colorAfter = intent.color))

    is StudioIntent.OnionSkin.SetOpacity -> copy(onionSkin = onionSkin.copy(opacity = intent.opacity.coerceIn(0f, 1f)))

    StudioIntent.OnionSkin.Reset -> copy(onionSkin = OnionSkinState())
}

/** Tool settings. Draw intents that bake, undo or switch tools are handled by the ViewModel. */
internal fun StudioState.reduceToolSetting(intent: StudioIntent.Draw): StudioState = when (intent) {
    is StudioIntent.Draw.SelectConstructionShape -> copy(
        canvas = canvas.copy(rulerMode = RulerMode.NONE, rulerGuide = null),
        tool = tool.copy(activeTool = DrawingTool.BRUSH, constructionShape = intent.shape),
    )

    is StudioIntent.Draw.SetBrushColor -> withActiveToolConfig { it.copy(color = intent.color) }

    is StudioIntent.Draw.SetBrushSize -> withActiveToolConfig { it.copy(size = intent.size.coerceIn(1f, 100f)) }

    is StudioIntent.Draw.SelectBrush -> withToolConfig(DrawingTool.BRUSH) { it.copy(brushId = intent.brushId) }

    is StudioIntent.Draw.SetEraserSize -> withToolConfig(DrawingTool.ERASER) {
        it.copy(size = intent.size.coerceIn(1f, 100f))
    }

    is StudioIntent.Draw.SetEraserType -> withToolConfig(DrawingTool.ERASER) { it.copy(eraserType = intent.type) }

    is StudioIntent.Draw.SetBrushOpacity -> withActiveToolConfig { it.copy(opacity = intent.opacity.coerceIn(0f, 1f)) }

    is StudioIntent.Draw.SetBrushStrength -> withActiveToolConfig {
        it.copy(strength = intent.strength.coerceIn(0f, 1f))
    }

    is StudioIntent.Draw.SetEraserCursor -> copy(tool = tool.copy(eraserCursor = intent.offset))

    is StudioIntent.Draw.BakeStroke, is StudioIntent.Draw.SelectTool, is StudioIntent.Draw.DrawStroke,
    StudioIntent.Draw.ClearAllHandwriting, StudioIntent.Draw.Undo, StudioIntent.Draw.Redo,
    -> this
}

/** Updates the active tool's settings, or the construction shape's while one is being drawn. */
internal fun StudioState.withActiveToolConfig(block: (ToolConfig) -> ToolConfig): StudioState {
    val toolState = tool
    val activeTool = toolState.activeTool
    return if (toolState.constructionShape != null) {
        copy(tool = toolState.copy(constructionConfig = block(toolState.constructionConfig)))
    } else {
        copy(
            tool = toolState.copy(
                configs =
                toolState.configs + (activeTool to block(toolState.configs[activeTool] ?: ToolConfig())),
            ),
        )
    }
}

/** Updates one tool's settings. Brush settings go to the construction shape while one is drawn. */
internal fun StudioState.withToolConfig(drawingTool: DrawingTool, block: (ToolConfig) -> ToolConfig): StudioState {
    val toolState = tool
    return if (toolState.constructionShape != null && drawingTool == DrawingTool.BRUSH) {
        copy(tool = toolState.copy(constructionConfig = block(toolState.constructionConfig)))
    } else {
        copy(
            tool = toolState.copy(
                configs =
                toolState.configs + (drawingTool to block(toolState.configs[drawingTool] ?: ToolConfig())),
            ),
        )
    }
}

/** Selection settings. Intents that change pixels or the clipboard are handled by the ViewModel. */
internal fun StudioState.reduceSelectionSetting(intent: StudioIntent.Selection): StudioState = when (intent) {
    is StudioIntent.Selection.SetTool -> copy(selection = selection.copy(toolType = intent.type))

    is StudioIntent.Selection.UpdateTransform -> copy(selection = selection.copy(transform = intent.matrix))

    is StudioIntent.Selection.UpdatePivot -> copy(selection = selection.copy(pivotOffset = intent.offset))

    is StudioIntent.Selection.SetContextMenuVisible -> copy(
        selection = selection.copy(contextMenuVisible = intent.visible),
    )

    is StudioIntent.Selection.SetInteractionMode ->
        copy(selection = selection.copy(interactionMode = intent.mode, contextMenuVisible = false))

    is StudioIntent.Selection.SetAspectLock -> copy(selection = selection.copy(lockAspectRatio = intent.locked))

    is StudioIntent.Selection.SetSkewMode -> copy(selection = selection.copy(skewMode = intent.enabled))

    StudioIntent.Selection.ResetTransform -> copy(
        selection = selection.copy(transform = Matrix(), pivotOffset = Offset.Zero),
    )

    is StudioIntent.Selection.SetDockPosition -> copy(canvas = canvas.copy(selectionDockOffset = intent.offset))

    is StudioIntent.Selection.SetPartialInclude -> copy(selection = selection.copy(partialInclude = intent.include))

    is StudioIntent.Selection.SelectArea, is StudioIntent.Selection.SelectTextNode, is StudioIntent.Selection.SetActive,
    StudioIntent.Selection.Cut, StudioIntent.Selection.Copy, StudioIntent.Selection.Paste,
    is StudioIntent.Selection.StampBitmap, StudioIntent.Selection.Delete, StudioIntent.Selection.Cancel,
    -> this
}

/** Fill settings. Starting a preview and confirming a fill are handled by the ViewModel. */
internal fun StudioState.reduceFillSetting(intent: StudioIntent.Fill): StudioState = when (intent) {
    is StudioIntent.Fill.SetDockPosition -> copy(fill = fill.copy(dockOffset = intent.offset))
    StudioIntent.Fill.Cancel -> clearFillPreview()
    is StudioIntent.Fill.SetPreviewBitmap -> copy(fill = fill.copy(previewBitmap = intent.bitmap))
    is StudioIntent.Fill.SetPendingTap, StudioIntent.Fill.Confirm -> this
}

internal fun StudioState.clearFillPreview(): StudioState =
    copy(fill = fill.copy(previewActive = false, pendingTap = null, previewBitmap = null))

/** Construction-shape preview settings. Confirming bakes the shape and is handled by the ViewModel. */
internal fun StudioState.reduceConstructionSetting(intent: StudioIntent.Construction): StudioState = when (intent) {
    is StudioIntent.Construction.SetPreview -> copy(
        constructionShape = constructionShape.copy(
            previewStrokes = intent.strokes,
            previewActive = intent.strokes.isNotEmpty(),
        ),
    )

    is StudioIntent.Construction.SetDockPosition -> copy(
        constructionShape = constructionShape.copy(dockOffset = intent.offset),
    )

    StudioIntent.Construction.Cancel, StudioIntent.Construction.Confirm -> clearConstructionPreview()
}

internal fun StudioState.clearConstructionPreview(): StudioState =
    copy(constructionShape = constructionShape.copy(previewStrokes = emptyList(), previewActive = false))

internal fun StudioState.toggleTrackGroup(group: TrackGroupKind): StudioState {
    val collapsed = tracks.collapsedGroups
    return copy(
        tracks = tracks.copy(
            collapsedGroups = if (group in
                collapsed
            ) {
                collapsed - group
            } else {
                collapsed + group
            },
        ),
    )
}

/**
 * Switches between drawing, rig setup and posing. Entering an actor mode remembers the drawing
 * layer to return to, and leaving one remembers the actor layer to come back to.
 */
internal fun StudioState.switchStudioMode(mode: StudioMode): StudioState {
    val layers = currentFrameLayers()
    val entering = scene.studioMode == StudioMode.RASTER_DRAWING && mode != StudioMode.RASTER_DRAWING
    val leaving = mode == StudioMode.RASTER_DRAWING
    val previousLayerId = layers.getOrNull(this.layers.activeLayerIndex)?.id
    val destination = if (leaving) {
        actor.returnLayerId
    } else if (entering) {
        actor.lastActorLayerId
    } else {
        previousLayerId
    }
    val destinationIndex =
        layers.indexOfFirst { it.id == destination }.takeIf { it >= 0 } ?: this.layers.activeLayerIndex
    return copy(
        layers = this.layers.copy(activeLayerIndex = destinationIndex),
        actor = actor.copy(
            showSource = mode != StudioMode.ANIMATE_IK,
            returnLayerId = if (entering) previousLayerId else actor.returnLayerId,
            lastActorLayerId = if (leaving) previousLayerId else actor.lastActorLayerId,
        ),
        scene = scene.copy(studioMode = mode),
        tool = if (mode == StudioMode.RASTER_DRAWING) {
            tool.copy(activeTool = DrawingTool.BRUSH)
        } else {
            tool.copy(activeTool = DrawingTool.RIG_SELECT, constructionShape = null)
        },
    )
}
