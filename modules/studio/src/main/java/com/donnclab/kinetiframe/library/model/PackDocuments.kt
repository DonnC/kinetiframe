package com.donnclab.kinetiframe.library.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One stamp-ready drawing inside a [PackManifest], matching the `.kpack`
 * spec key-for-key (snake_case `file`/`index`/`name`/`desc`) so the internal `payload/meta.json`
 * this app writes and the external, WhatsApp/Telegram-shareable `.kpack` file
 * (`PackArchiver`) are byte-for-byte the same JSON -- no translation step needed at the
 * export/import boundary, mirroring how `.kframe` is just a zip of the working directory as-is.
 */
@Serializable
data class PackItemEntry(
    val file: String,
    val index: Int,
    val name: String,
    val desc: String = "",
    val registration: PackArtworkRegistration? = null,
) {
    companion object {
        const val MAX_NAME_LENGTH = 40
        const val MAX_DESCRIPTION_LENGTH = 200
    }
    fun validate(): PackItemEntry = apply {
        require(file.isNotBlank()) { "Every pack item needs a file name" }
        require(
            file != "." && file != ".." && file.none {
                it == '/' || it == '\\' || it == ':'
            },
        ) { "Pack drawing must use a safe filename" }
        require(index >= 0) { "Pack item index cannot be negative" }
        require(name.isNotBlank() && name.length <= MAX_NAME_LENGTH) {
            "Pack item name must contain 1-$MAX_NAME_LENGTH characters"
        }
        require(desc.length <= MAX_DESCRIPTION_LENGTH) { "Pack item description is too long" }
        registration?.validate()
    }
}

/**
 * The pack's own index -- who made it, how big its square canvas is, and which named drawing each
 * `frame_NNN.png` actually is. This is the literal `.kpack` `meta.json` schema; a pack's internal
 * storage keeps this exact same file at `payload/meta.json` inside its `library/<itemId>/` (or
 * project vault) directory.
 */
@Serializable
data class PackManifest(
    @SerialName("pack_name") val packName: String,
    val author: String = "",
    @SerialName("total_items") val totalItems: Int,
    @SerialName("canvas_dimension") val canvasDimension: Int,
    val items: List<PackItemEntry>,
) {
    fun validate(): PackManifest = apply {
        require(packName.isNotBlank() && packName.length <= 60) { "Pack name must contain 1-60 characters" }
        require(canvasDimension > 0) { "Pack canvas dimension must be positive" }
        require(items.isNotEmpty()) { "A pack must contain at least one item" }
        require(items.size == totalItems) { "total_items does not match the number of items" }
        val fileNames = items.map { it.file }
        require(fileNames.toSet().size == fileNames.size) { "Pack item file names must be unique" }
        items.forEach { it.validate() }
    }
}
