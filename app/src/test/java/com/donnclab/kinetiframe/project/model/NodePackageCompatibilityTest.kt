package com.donnclab.kinetiframe.project.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class NodePackageCompatibilityTest {
    @Test
    fun `legacy text node type still opens and saves after source package migration`() {
        val wire = """{"type":"com.kinetiframe.models.TextNode","id":"text","text":"Hello"}"""
        val node = Json.decodeFromString<BaseNode>(wire)
        assertEquals("Hello", assertInstanceOf(TextNode::class.java, node).text)
        assertLegacyRoundTrip(node, "com.kinetiframe.models.TextNode")
    }

    @Test
    fun `legacy stroke node type still opens and saves after source package migration`() {
        val wire = """{
            "type":"com.kinetiframe.models.StrokeNode","id":"stroke",
            "command":{"points":[],"tool":"BRUSH","color":0,"width":1.0}
        }"""
        val node = Json.decodeFromString<BaseNode>(wire)
        assertInstanceOf(StrokeNode::class.java, node)
        assertLegacyRoundTrip(node, "com.kinetiframe.models.StrokeNode")
    }

    private fun assertLegacyRoundTrip(node: BaseNode, type: String) {
        val encoded = Json.encodeToString(node)
        assertEquals(type, Json.parseToJsonElement(encoded).jsonObject.getValue("type").jsonPrimitive.content)
        assertEquals(node, Json.decodeFromString<BaseNode>(encoded))
    }
}
