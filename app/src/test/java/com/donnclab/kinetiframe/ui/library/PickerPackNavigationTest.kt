package com.donnclab.kinetiframe.ui.library

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PickerPackNavigationTest {
    @Test fun `actor attachment keeps parent flow alive before opening pack items`() {
        val events = mutableListOf<String>()
        openPackFromPicker(false, { events += "dismiss" }, { events += "items" })
        assertEquals(listOf("items"), events)
    }

    @Test fun `ordinary stamp tray still dismisses library before opening pack`() {
        val events = mutableListOf<String>()
        openPackFromPicker(true, { events += "dismiss" }, { events += "tray" })
        assertEquals(listOf("dismiss", "tray"), events)
    }
}
