package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.core.actor.document.ActorRevision
import com.donnclab.kinetiframe.core.actor.validation.ActorGraphValidation
import com.donnclab.kinetiframe.library.data.PackManifest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

class CuratedPackCatalogTest {
    @Test fun `directional starter views validate and pack pixels match their rigged artwork`() {
        val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
        val json = Json { ignoreUnknownKeys = true }
        ZipFile(
            com.donnclab.kinetiframe.library.bundled.DemoAssetFixtures.file(
                assets,
                BundledActorSourceReader.ASSET_PATH,
            ),
        ).use { archive ->
            listOf("tavi", "mira", "kuda").forEach { character ->
                val id = "ls-$character-all-views-v3"
                val footprints = mutableSetOf<String>()
                ZipFile(
                    com.donnclab.kinetiframe.library.bundled.DemoAssetFixtures.file(
                        assets,
                        "loose-signal/curated/v4/ls-$character-cutouts-v6.kpack",
                    ),
                ).use { pack ->
                    val manifest = json.decodeFromString<PackManifest>(
                        pack.getInputStream(pack.getEntry("meta.json")).bufferedReader().readText(),
                    )
                    listOf("front", "profile", "three_quarter", "back").forEach { view ->
                        val prefix = "dependencies/actors/$id/"
                        val revision = json.decodeFromString<ActorRevision>(
                            archive.getInputStream(
                                archive.getEntry("${prefix}revisions/$id-$view-joined-v4.json"),
                            ).bufferedReader().readText(),
                        ).validate()
                        ActorGraphValidation.validate(revision.sceneGraph)
                        if (view ==
                            "back"
                        ) {
                            assertFalse(revision.sceneGraph.bones.keys.any { it in setOf("eyes", "mouth") })
                        }
                        if (view == "profile") {
                            listOf("upper_arm", "thigh").forEach { part ->
                                assertEquals(
                                    revision.sceneGraph.bones.getValue("left_$part").localTransform.y,
                                    revision.sceneGraph.bones.getValue("right_$part").localTransform.y,
                                )
                            }
                        }
                        listOf(
                            "head",
                            "torso",
                            "right_foot",
                            "left_hand",
                            "left_hand#palm",
                            "left_hand#point",
                            "left_hand#fist",
                            "right_hand", "right_hand#palm", "right_hand#point", "right_hand#fist",
                        ).forEach { part ->
                            val source = archive.getInputStream(
                                archive.getEntry(prefix + revision.artworkAssetPaths.getValue(part)),
                            ).readBytes()
                            val item = manifest.items.single {
                                it.name ==
                                    "${view.replace('_', '-')} · ${part.replace('_', ' ').replace("#", " · ")}"
                            }
                            assertArrayEquals(source, pack.getInputStream(pack.getEntry(item.file)).readBytes())
                            if (part ==
                                "right_foot"
                            ) {
                                footprints +=
                                    MessageDigest.getInstance("SHA-256").digest(source).joinToString("") {
                                        "%02x".format(it)
                                    }
                            }
                        }
                    }
                }
                assertEquals(4, footprints.size, "Directional shoes cannot reuse one side silhouette")
            }
        }
    }

    @Test fun `seven curated packs have valid names complete view groups and independent facial artwork`() {
        val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
        assertEquals(listOf(BundledDemoInstaller.TAVI_PRESENTATION_ID), BundledDemoInstaller.projectIds)
        assertEquals(7, BundledDemoInstaller.packIds.size)
        BundledDemoInstaller.packIds.forEach { id ->
            val folder = if (id.contains("cutouts-v6")) "v4" else "v3"
            ZipFile(
                com.donnclab.kinetiframe.library.bundled.DemoAssetFixtures.file(
                    assets,
                    "loose-signal/curated/$folder/$id.kpack",
                ),
            ).use { zip ->
                val manifest = Json.decodeFromString<PackManifest>(
                    zip.getInputStream(zip.getEntry("meta.json")).bufferedReader().readText(),
                ).validate()
                manifest.items.forEach { assertNotNull(zip.getEntry(it.file), "$id/${it.file}") }
                if (id.contains("cutouts")) {
                    assertEquals(80, manifest.totalItems)
                    listOf("front", "profile", "three-quarter", "back").forEach { view ->
                        assertEquals(20, manifest.items.count { it.name.startsWith("$view · ") })
                        for (side in listOf("left", "right")) {
                            for (shape in listOf("", " · palm", " · point", " · fist")) {
                                assertEquals(1, manifest.items.count { it.name == "$view · $side hand$shape" })
                            }
                        }
                    }
                    assertFalse(manifest.items.any { it.name.contains("eyes") || it.name.contains("mouth") })
                }
                if (id == "ls-expressions-v3") assertEquals(12, manifest.totalItems)
                if (id == "ls-eyes-v3") assertEquals(6, manifest.totalItems)
                if (id == "ls-mouths-v3") assertEquals(14, manifest.totalItems)
                if (id == "ls-studio-items-v3") assertEquals(11, manifest.totalItems)
            }
        }
    }
}
