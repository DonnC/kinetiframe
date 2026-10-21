// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PresetRecipeValidationTest {
    private val rotation = PresetRotationChannel("root", points = listOf(RotationPoint(0f, 0f), RotationPoint(1f, 0f)))
    private val root = RootMotionChannel("root", listOf(RootMotionPoint(0f), RootMotionPoint(1f)))
    private val recipe = PresetRecipe(
        PresetRecipeMetadata(3, "test", "Test", 1f),
        PresetRecipeChannels(listOf("root"), listOf(rotation), listOf(root)),
    )

    @Test fun `identity schema and duration limits are inclusive and typed`() {
        listOf(.25f, 30f).forEach { duration ->
            assertDoesNotThrow {
                PresetRecipeValidation.requireValid(
                    recipe.copy(metadata = recipe.metadata.copy(id = "a".repeat(80), durationSeconds = duration)),
                )
            }
        }
        assertEquals(
            PresetRecipeIssue.UNSUPPORTED_SCHEMA,
            failure(recipe.copy(metadata = recipe.metadata.copy(schemaVersion = 4, id = ""))),
        )
        assertEquals(
            PresetRecipeIssue.INVALID_IDENTITY,
            failure(recipe.copy(metadata = recipe.metadata.copy(id = "BAD"))),
        )
        assertEquals(
            PresetRecipeIssue.INVALID_DURATION,
            failure(recipe.copy(metadata = recipe.metadata.copy(durationSeconds = Float.NaN))),
        )
    }

    @Test fun `rotation and role checks retain count uniqueness and permissive role semantics`() {
        assertEquals(
            PresetRecipeIssue.ROTATION_COUNT,
            failure(recipe.copy(channels = recipe.channels.copy(rotations = emptyList()))),
        )
        assertEquals(
            PresetRecipeIssue.DUPLICATE_ROTATIONS,
            failure(recipe.copy(channels = recipe.channels.copy(rotations = listOf(rotation, rotation)))),
        )
        assertEquals(
            PresetRecipeIssue.INVALID_REQUIRED_BONES,
            failure(recipe.copy(channels = recipe.channels.copy(requiredBones = emptyList()))),
        )
        val many = (0 until 128).map { rotation.copy(boneId = "part-$it") }
        assertDoesNotThrow {
            PresetRecipeValidation.requireValid(recipe.copy(channels = recipe.channels.copy(rotations = many)))
        }
        assertEquals(
            PresetRecipeIssue.ROTATION_COUNT,
            failure(
                recipe.copy(
                    channels = recipe.channels.copy(
                        rotations =
                        many + rotation,
                    ),
                ),
            ),
        )
        assertDoesNotThrow {
            PresetRecipeValidation.requireValid(
                recipe.copy(channels = PresetRecipeChannels(listOf(""), listOf(rotation))),
            )
        }
    }

    @Test fun `root schema count uniqueness and required role rules stay separate`() {
        assertEquals(
            PresetRecipeIssue.ROOT_SCHEMA,
            failure(recipe.copy(metadata = recipe.metadata.copy(schemaVersion = 2))),
        )
        assertEquals(
            PresetRecipeIssue.ROOT_COUNT,
            failure(recipe.copy(channels = recipe.channels.copy(roots = listOf(root, root)))),
        )
        val unknown = root.copy(boneId = "unknown")
        assertEquals(
            PresetRecipeIssue.INVALID_ROOT_CHANNEL,
            failure(recipe.copy(channels = recipe.channels.copy(roots = listOf(unknown)))),
        )
        val roots = (0 until 16).map { root.copy(boneId = "root-$it") }
        assertDoesNotThrow {
            PresetRecipeValidation.requireValid(
                recipe.copy(
                    channels = recipe.channels.copy(
                        requiredBones = roots.map {
                            it.boneId
                        },
                        roots = roots,
                    ),
                ),
            )
        }
        assertEquals(
            PresetRecipeIssue.ROOT_COUNT,
            failure(
                recipe.copy(
                    channels = recipe.channels.copy(
                        roots =
                        roots + root,
                    ),
                ),
            ),
        )
    }

    @Test fun `root endpoint values spacing and seam rules retain thresholds`() {
        fun withPoints(points: List<RootMotionPoint>) =
            recipe.copy(channels = recipe.channels.copy(roots = listOf(root.copy(points = points))))
        assertEquals(PresetRecipeIssue.INVALID_ROOT_CHANNEL, failure(withPoints(emptyList())))
        assertEquals(
            PresetRecipeIssue.INVALID_ROOT_ENDPOINTS,
            failure(withPoints(listOf(RootMotionPoint(.1f), RootMotionPoint(1f)))),
        )
        assertEquals(
            PresetRecipeIssue.INVALID_ROOT_POINT,
            failure(withPoints(listOf(RootMotionPoint(0f, 4.1f), RootMotionPoint(1f)))),
        )
        assertEquals(
            PresetRecipeIssue.UNORDERED_ROOT_POINTS,
            failure(withPoints(listOf(RootMotionPoint(0f), RootMotionPoint(.0000009f), RootMotionPoint(1f)))),
        )
        assertDoesNotThrow {
            PresetRecipeValidation.requireValid(
                withPoints(listOf(RootMotionPoint(0f), RootMotionPoint(.000001f, 4f, -4f), RootMotionPoint(1f))),
            )
        }
        val differentEnd = withPoints(listOf(RootMotionPoint(0f), RootMotionPoint(1f, 1f)))
        assertEquals(PresetRecipeIssue.ROOT_LOOP_ENDPOINTS, failure(differentEnd))
        assertDoesNotThrow { PresetRecipeValidation.requireValid(differentEnd.copy(loop = false)) }
    }

    @Test fun `root validation precedes rotation validation and delegates channel errors`() {
        val invalid = recipe.copy(
            channels = recipe.channels.copy(
                rotations = listOf(rotation.copy(boneId = "")),
                roots = listOf(root.copy(points = emptyList())),
            ),
        )
        assertEquals(PresetRecipeIssue.INVALID_ROOT_CHANNEL, failure(invalid))
        val error = assertThrows(PresetChannelValidationException::class.java) {
            PresetRecipeValidation.requireValid(invalid.copy(channels = invalid.channels.copy(roots = emptyList())))
        }
        assertEquals(PresetChannelIssue.EMPTY_PART_ID, error.issue)
    }

    @Test fun `validation returns the same input without sorting repairing or publishing`() {
        assertSame(recipe, PresetRecipeValidation.requireValid(recipe))
        assertEquals(listOf(root), recipe.channels.roots)
        assertEquals(listOf(rotation), recipe.channels.rotations)
    }

    private fun failure(value: PresetRecipe): PresetRecipeIssue =
        assertThrows(PresetRecipeValidationException::class.java) { PresetRecipeValidation.requireValid(value) }.issue
}
