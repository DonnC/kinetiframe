package com.donnclab.kinetiframe.studio

import com.donnclab.kinetiframe.project.model.DrawingTool
import com.donnclab.kinetiframe.project.model.Layer
import com.donnclab.kinetiframe.project.model.TextNode
import com.donnclab.kinetiframe.project.model.TouchPoint

/**
 * Handles intents for the drawing surface: playback, drawing, layers, frames, rigging, text,
 * selection, fill and construction shapes.
 */
internal class StudioCanvasIntents(private val store: StudioStore, private val session: StudioSession) {
    fun handlePlaybackIntent(intent: StudioIntent.Playback) {
        if (store.state.scene.studioMode != StudioMode.RASTER_DRAWING) return
        when (intent) {
            StudioIntent.Playback.Play -> session.playbackManager.play(store.state)

            StudioIntent.Playback.Pause -> session.playbackManager.pause(store.state)

            StudioIntent.Playback.NextFrame -> session.playbackManager.incrementFrame(store.state)

            StudioIntent.Playback.PreviousFrame -> session.playbackManager.decrementFrame(store.state)

            is StudioIntent.Playback.SetFps -> session.playbackManager.setFps(intent.fps)

            is StudioIntent.Playback.SetCurrentFrame -> {
                if (store.state.animation.isPlaying) session.playbackManager.pause(store.state)
                session.playbackManager.setCurrentFrame(intent.frame, store.state.animation.totalFrames)
            }
        }
    }

    fun handleDrawIntent(intent: StudioIntent.Draw) {
        if (store.state.actor.isPerformanceAuthoring) return
        when (intent) {
            is StudioIntent.Draw.BakeStroke -> {
                val targetLayerId = intent.targetBoneId?.let(session::selectActorArtworkLayer)
                session.drawingManager.bakeStroke(intent.points, intent.brush, targetLayerId)
            }

            is StudioIntent.Draw.SelectTool -> {
                val rigTool = intent.tool in setOf(DrawingTool.LASSO_CUT, DrawingTool.BONE, DrawingTool.RIG_SELECT) ||
                    (intent.tool == DrawingTool.LASSO && store.state.scene.studioMode == StudioMode.RIG_SETUP)
                if (rigTool != (store.state.scene.studioMode != StudioMode.RASTER_DRAWING)) return
                store.update {
                    it.copy(tool = it.tool.copy(activeTool = intent.tool, constructionShape = null)).clearFillPreview()
                }
            }

            StudioIntent.Draw.ClearAllHandwriting -> session.drawingManager.clearAllHandwriting()

            StudioIntent.Draw.Undo -> session.drawingManager.undo()

            StudioIntent.Draw.Redo -> session.drawingManager.redo()

            is StudioIntent.Draw.DrawStroke -> { /* UI handle */ }

            else -> store.update { it.reduceToolSetting(intent) }
        }
    }

    fun handleLayerIntent(intent: StudioIntent.Layer) {
        if (intent !is StudioIntent.Layer.SelectLayer) return session.layerManager.handle(intent)
        if (intent.index in store.state.currentFrameLayers().indices) {
            if (store.state.scene.studioMode != StudioMode.RASTER_DRAWING) {
                session.actorAuthoring.cancelInFlightAuthoring()
            }
            store.update { it.copy(layers = it.layers.copy(activeLayerIndex = intent.index)) }
            session.loadFrame(store.state.animation.currentFrame)
        }
    }

    fun handleFrameIntent(intent: StudioIntent.Frame) {
        when (intent) {
            is StudioIntent.Frame.HoldDrawing -> session.celHoldManager.hold(intent.first, intent.last)
            is StudioIntent.Frame.EditDrawingScope -> session.celHoldManager.editScope(intent.index, intent.allLinked)
            is StudioIntent.Frame.UnlinkDrawing -> session.celHoldManager.unlink(intent.index)
            else -> session.frameManager.handle(intent)
        }
    }

    fun handleRiggingIntent(intent: StudioIntent.Rigging) {
        if (store.state.actor.isPerformanceAuthoring && intent !is StudioIntent.Rigging.SelectBone &&
            intent != StudioIntent.Rigging.ToggleSkeletonOverlay &&
            intent != StudioIntent.Rigging.SetStudioMode(StudioMode.ANIMATE_IK)
        ) {
            return
        }
        when (intent) {
            is StudioIntent.Rigging.SetStudioMode -> {
                // A project-launched Performance Stage session may record motion only. The
                // actor revision it renders is immutable so prior gestures remain reproducible.
                if (store.state.actor.isPerformanceAuthoring && intent.mode != StudioMode.ANIMATE_IK) return
                session.actorAuthoring.cancelInFlightAuthoring()
                if (intent.mode == StudioMode.RASTER_DRAWING) session.actorAuthoring.clearHistory()
                if (intent.mode != StudioMode.RASTER_DRAWING) session.playbackManager.pause(store.state)
                store.update { it.switchStudioMode(intent.mode) }
                if (intent.mode == StudioMode.RASTER_DRAWING &&
                    session.shouldInitialize
                ) {
                    session.loadFrame(store.state.animation.currentFrame)
                }
            }

            is StudioIntent.Rigging.PreparePivotPlacement -> session.actorAuthoring.preparePivotPlacement(
                intent.sourceFrame,
                intent.lassoPath,
                intent.dilationRadius,
            )

            is StudioIntent.Rigging.MovePivot -> session.actorAuthoring.updatePivot(intent.x, intent.y)

            is StudioIntent.Rigging.AddBoneAt -> session.actorAuthoring.addBoneAt(intent.x, intent.y)

            is StudioIntent.Rigging.SelectBone -> {
                if (store.state.scene.pivotPlacement != null &&
                    store.state.scene.pivotPlacement?.targetBoneId != intent.id
                ) {
                    session.actorAuthoring.cancelPivot()
                }
                val boneId = intent.id
                // WHY this is not gated to RIG_SETUP: RIG_SETUP is the *pivot-adjust* tool state
                // (the Bone button); painting happens in RASTER_DRAWING. Arming the part's layer
                // only in RIG_SETUP meant the blueprint's core flow -- "tap the Left Forearm slot,
                // paint over its bone line, engine binds silently" -- could never fire from the
                // brush, because the layer carrying `boundBoneId` (the trigger for
                // RiggingManager.autoBindLayerContent on every bake) was never made active while
                // drawing. Selecting a part now arms and focuses its layer in either authoring
                // mode; ANIMATE_IK stays excluded because posing must never mint raster layers,
                // and the actorDefinition guard keeps a plain FxF project from doing so at all.
                if (boneId != null) session.selectActorArtworkLayer(boneId)
                store.update { state ->
                    state.copy(scene = state.scene.copy(selectedBoneId = boneId))
                }
            }

            StudioIntent.Rigging.ConfirmPivot -> session.actorAuthoring.confirmPivot()

            StudioIntent.Rigging.CancelPivot -> session.actorAuthoring.cancelPivot()

            StudioIntent.Rigging.ToggleSkeletonOverlay -> store.update { state ->
                state.copy(scene = state.scene.copy(skeletonOverlayVisible = !state.scene.skeletonOverlayVisible))
            }
        }
    }

    fun handleTextIntent(intent: StudioIntent.Text) {
        when (intent) {
            is StudioIntent.Text.AddText -> session.textManager.addText(intent.text, intent.x, intent.y)

            is StudioIntent.Text.UpdateText -> session.textManager.updateText(intent.id, intent.text)

            is StudioIntent.Text.SelectTextNode -> session.textManager.selectTextNode(intent.id)

            is StudioIntent.Text.DeleteTextNode -> session.textManager.deleteTextNode(intent.id)

            is StudioIntent.Text.SetTextToolOptions -> store.update {
                it.copy(tool = it.tool.copy(textToolOptions = intent.options))
            }

            is StudioIntent.Text.UpdateTextNode -> session.textManager.updateTextNode(intent.node)

            is StudioIntent.Text.TriggerEditDialog -> {
                // The node may be detached from SceneState while SelectionManager owns
                // it for a transform. Both states represent the same editable node.
                val node = (
                    store.state.scene.nodes[intent.id]
                        ?: store.state.selection.selectedContent?.nodes?.get(intent.id)
                    ) as? TextNode ?: return
                store.send(StudioEffect.ShowTextEditDialog(intent.id, node.text))
            }

            is StudioIntent.Text.TriggerAddTextDialog -> {
                store.send(StudioEffect.ShowTextAddDialog(intent.x, intent.y))
            }
        }
    }

    fun handleSelectionIntent(intent: StudioIntent.Selection) {
        when (intent) {
            is StudioIntent.Selection.SelectArea -> session.selectionManager.selectArea(
                intent.path,
                intent.rect,
                intent.density,
                intent.fontRegistry,
                intent.actors,
            )

            is StudioIntent.Selection.SelectTextNode -> session.selectionManager.selectTextNode(
                intent.id,
                intent.density,
                intent.fontRegistry,
            )

            is StudioIntent.Selection.SetActive -> {
                if (!intent.active && store.state.selection.active) {
                    session.selectionManager.commitSelection()
                } else {
                    store.update {
                        it.copy(
                            selection = it.selection.copy(
                                active = intent.active,
                                contextMenuVisible = if (intent.active) it.selection.contextMenuVisible else false,
                            ),
                        )
                    }
                }
            }

            StudioIntent.Selection.Cut -> session.selectionManager.cutSelection()

            StudioIntent.Selection.Copy -> {
                if (store.state.selection.active) {
                    session.selectionManager.copySelection()
                } else {
                    session.frameManager.copyFrame(store.state.animation.currentFrame)
                }
            }

            StudioIntent.Selection.Paste -> {
                if (session.selectionManager.hasClipboardContent()) {
                    session.selectionManager.pasteSelection()
                } else {
                    session.frameManager.pasteFrame(store.state.animation.currentFrame, before = false)
                }
            }

            is StudioIntent.Selection.StampBitmap -> session.selectionManager.pasteBitmap(
                intent.bitmap,
                store.state.project.stage.width,
                store.state.project.stage.height,
                intent.centerX,
                intent.centerY,
            )

            StudioIntent.Selection.Delete -> session.selectionManager.deleteSelection()

            StudioIntent.Selection.Cancel -> session.selectionManager.cancelSelection()

            else -> store.update { it.reduceSelectionSetting(intent) }
        }
    }

    fun handleFillIntent(intent: StudioIntent.Fill) {
        when (intent) {
            is StudioIntent.Fill.SetPendingTap -> {
                store.update {
                    it.copy(
                        fill = it.fill.copy(
                            pendingTap = intent.offset,
                            previewActive =
                            intent.offset != null,
                        ),
                    )
                }
                if (intent.offset != null) {
                    session.generateFillPreview(intent.offset)
                }
            }

            StudioIntent.Fill.Confirm -> {
                val tap = store.state.fill.pendingTap
                if (tap != null) {
                    session.drawingManager.bakeStroke(listOf(TouchPoint(tap.x, tap.y)))
                }
                store.update { it.clearFillPreview() }
            }

            else -> store.update { it.reduceFillSetting(intent) }
        }
    }

    fun handleConstructionShapeIntent(intent: StudioIntent.Construction) {
        if (intent == StudioIntent.Construction.Confirm) {
            session.drawingManager.bakeConstructionShape(store.state.constructionShape.previewStrokes)
        }
        store.update { it.reduceConstructionSetting(intent) }
    }
}
