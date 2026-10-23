package com.donnclab.kinetiframe.actors.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.donnclab.kinetiframe.actors.data.ActorRevision
import com.donnclab.kinetiframe.actors.rig.model.ActorArtwork
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.core.document.PuppetInstance
import com.donnclab.kinetiframe.data.storage.DurableFile
import java.io.File

/** IO-only, revision-keyed whole-character thumbnail. Decodes one source part at a time. */
object ActorThumbnailMaker {
    fun create(root: File, revision: ActorRevision): String? {
        val output = File(root, "previews/${revision.revisionId}-whole-v1.png")
        if (output.isFile) return output.absolutePath
        val bindings = ActorArtwork.bindings(revision.sceneGraph)
        val sources = revision.artworkAssetPaths.mapNotNull { (address, path) ->
            val file = File(root, path).canonicalFile
            if (!file.path.startsWith(root.canonicalPath + File.separator) || !file.isFile) return@mapNotNull null
            bindings[address]?.assetId?.let { it to file }
        }.toMap()
        val commands = PuppetRenderPlan.create(
            PuppetDefinition("thumbnail", "Preview", revision.sceneGraph),
            PuppetInstance("preview", "thumbnail"),
        )
        val sizes = sources.mapValues { (_, file) ->
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                BitmapFactory.decodeFile(file.path, this)
            }
        }
        val points = commands.flatMap { command ->
            val size = sizes[command.assetId] ?: return@flatMap emptyList()
            if (size.outWidth <= 0 || size.outHeight <= 0) return@flatMap emptyList()
            PuppetArtworkRenderer.outline(command, size.outWidth, size.outHeight).toList().chunked(2)
        }
        if (points.isEmpty()) return null
        val left = points.minOf { it[0] }
        val top = points.minOf { it[1] }
        val width = (points.maxOf { it[0] } - left).coerceAtLeast(1f)
        val height = (points.maxOf { it[1] } - top).coerceAtLeast(1f)
        val fit = minOf(288f / width, 288f / height)
        val result = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(result)
            canvas.translate((320 - width * fit) / 2 - left * fit, (320 - height * fit) / 2 - top * fit)
            canvas.scale(fit, fit)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            commands.forEach { command ->
                val size = sizes[command.assetId] ?: return@forEach
                // Refuse giant imported parts rather than allocate an unbounded thumbnail heap.
                require(size.outWidth.toLong() * size.outHeight <= 8_000_000) { "Artwork is too large for preview" }
                val bitmap = sources[command.assetId]?.let { BitmapFactory.decodeFile(it.path) } ?: return@forEach
                try {
                    PuppetArtworkRenderer.draw(canvas, command, bitmap, paint)
                } finally {
                    bitmap.recycle()
                }
            }
            DurableFile.write(output) { check(result.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            return output.absolutePath
        } finally {
            result.recycle()
        }
    }
}
