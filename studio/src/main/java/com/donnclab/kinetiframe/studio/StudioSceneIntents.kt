package com.donnclab.kinetiframe.studio

import kotlinx.coroutines.launch

/** Handles intents for the scene around the drawing: camera moves, pack items, audio and video clips. */
class StudioSceneIntents(private val store: StudioStore, private val session: StudioSession) {
    fun handleCamera(intent: StudioIntent.Camera) {
        session.playbackManager.pause(store.state)
        runCatching {
            when (intent) {
                is StudioIntent.Camera.Put -> session.sceneEffects.put(intent.shot)
                is StudioIntent.Camera.Remove -> session.sceneEffects.remove(intent.id)
                StudioIntent.Camera.Undo -> session.sceneEffects.undo()
                StudioIntent.Camera.Redo -> session.sceneEffects.redo()
            }
        }.onFailure { error ->
            store.scope.launch { store.send(StudioEffect.ShowError(error.message ?: "Could not edit camera")) }
        }
    }

    fun handlePackItem(intent: StudioIntent.PackItem) {
        when (intent) {
            is StudioIntent.PackItem.SetTag -> store.update {
                it.copy(
                    packTags =
                    it.packTags + (intent.index to intent.tag),
                )
            }

            is StudioIntent.PackItem.Add -> session.frameManager.addFrameAt(intent.index, intent.tag)
        }
    }

    fun handleAudio(intent: StudioIntent.Audio) {
        when (intent) {
            is StudioIntent.Audio.VoiceStyle -> session.sceneEffects.voiceStyle(
                intent.clipId,
                intent.expectedAssetId,
                intent.effect,
                intent.proxy,
            )

            is StudioIntent.Audio.Crossfade -> session.sceneEffects.crossfade(
                intent.clipId,
                intent.nextClipId,
                intent.frames,
            )

            is StudioIntent.Audio.SetFades -> session.sceneEffects.fades(
                intent.clipId,
                intent.fadeInFrames,
                intent.fadeOutFrames,
            )

            else -> session.audioManager.handle(intent)
        }
    }

    fun handleVideo(intent: StudioIntent.Video) {
        when (intent) {
            is StudioIntent.Video.Framing -> session.sceneEffects.videoFraming(
                intent.clipId,
                intent.fit,
                intent.cropX,
                intent.cropY,
            )

            is StudioIntent.Video.Split -> session.sceneEffects.splitVideo(intent.clipId, intent.atFrame)

            is StudioIntent.Video.DetachAudio -> session.videoTrackManager.detachAudio(intent.clipId)

            is StudioIntent.Video.Add -> {
                session.videoTrackManager.add(intent.placement, intent.asset, intent.sourceAudio)
            }

            is StudioIntent.Video.Remove -> {
                if (intent.clipId in store.state.selection.selectedContent?.videos.orEmpty()) {
                    store.scope.launch {
                        session.selectionManager.cancelSelection().join()
                        session.videoTrackManager.remove(intent.clipId)
                    }
                } else {
                    session.videoTrackManager.remove(intent.clipId)
                }
            }

            is StudioIntent.Video.Select -> {
                store.state.project.videoClips[intent.clipId]?.let { clip ->
                    session.playbackManager.pause(store.state)
                    if (store.state.animation.currentFrame !in clip.timelineStartFrame until clip.timelineEndFrame) {
                        session.playbackManager.setCurrentFrame(
                            clip.timelineStartFrame,
                            store.state.animation.totalFrames,
                        )
                    }
                    session.selectionManager.selectVideo(intent.clipId)
                }
            }

            is StudioIntent.Video.Trim -> {
                if (intent.clipId in store.state.selection.selectedContent?.videos.orEmpty()) {
                    store.scope.launch {
                        session.selectionManager.commitSelection().join()
                        session.videoTrackManager.trim(
                            intent.clipId,
                            intent.timelineStartFrame,
                            intent.timelineEndFrame,
                            intent.videoSourceOffsetSeconds,
                        )
                    }
                } else {
                    session.videoTrackManager.trim(
                        intent.clipId,
                        intent.timelineStartFrame,
                        intent.timelineEndFrame,
                        intent.videoSourceOffsetSeconds,
                    )
                }
            }

            is StudioIntent.Video.SetTrackOpacity -> session.videoTrackManager.setTrackOpacity(intent.opacity)
        }
    }
}
