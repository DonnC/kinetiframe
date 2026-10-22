package com.donnclab.kinetiframe.library.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PackArtworkRegistrationTest {
    @Test fun `registration survives pack roundtrip and accounts for decode scaling`() {
        val registration = PackArtworkRegistration("arm", 200, 100, 50f, 20f, 10f, 5f, 3f, 90f, .5f, "forearm")
        val item = PackItemEntry("frame_000.png", 0, "Arm", registration = registration)
        val restored = Json.decodeFromString<PackItemEntry>(Json.encodeToString(item)).validate()
        assertEquals(item, restored)
        val binding = restored.registration!!.alignment("arm", 100f, 100, 50)!!
        assertEquals(10f, binding.pivotX)
        assertEquals(5f, binding.pivotY)
        assertEquals(10f, binding.offsetX)
        assertEquals(2f, binding.scale)
        assertNull(registration.alignment("head", 100f, 100, 50))
    }

    @Test fun `old packs remain valid without registration`() {
        assertNull(
            Json.decodeFromString<PackItemEntry>(
                """{"file":"frame_000.png","index":0,"name":"Old artwork"}""",
            ).validate().registration,
        )
    }
}
