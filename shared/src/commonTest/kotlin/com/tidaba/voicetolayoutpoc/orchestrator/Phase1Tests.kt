package com.tidaba.voicetolayoutpoc.orchestrator

import com.tidaba.voicetolayoutpoc.orchestrator.models.ComponentInstance
import com.tidaba.voicetolayoutpoc.orchestrator.models.LayoutManifest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ComponentInstanceTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testComponentInstanceDefaultId() {
        val component = ComponentInstance(name = "Button")
        assertTrue(component.id.isNotEmpty(), "ID should be auto-generated")
        assertEquals(32, component.id.length, "ID should be 32-char hex string")
    }

    @Test
    fun testComponentInstanceUniqueIds() {
        val ids = (1..100).map { ComponentInstance(name = "Button").id }.toSet()
        assertEquals(100, ids.size, "All 100 generated IDs should be unique")
    }

    @Test
    fun testComponentInstanceSerialization() {
        val component = ComponentInstance(
            id = "test-123",
            name = "Button",
            properties = mapOf("label" to "Submit", "color" to "Primary"),
            xOffset = 100.5f,
            yOffset = 200.0f,
        )

        val jsonString = json.encodeToString(ComponentInstance.serializer(), component)
        val decoded = json.decodeFromString(ComponentInstance.serializer(), jsonString)

        assertEquals(component, decoded)
    }

    @Test
    fun testComponentInstanceDefaults() {
        val component = ComponentInstance(name = "Text")

        assertEquals(emptyMap(), component.properties)
        assertEquals(0f, component.xOffset)
        assertEquals(0f, component.yOffset)
        assertEquals(null, component.width)
        assertEquals(null, component.height)
    }

    @Test
    fun testComponentInstanceWithAllFields() {
        val component = ComponentInstance(
            id = "full-test",
            name = "Card",
            properties = mapOf("elevation" to "2", "cornerRadius" to "16"),
            xOffset = 50f,
            yOffset = 75f,
            width = 200f,
            height = 300f,
        )

        val jsonString = json.encodeToString(ComponentInstance.serializer(), component)
        val decoded = json.decodeFromString(ComponentInstance.serializer(), jsonString)

        assertEquals(component, decoded)
        assertEquals(200f, decoded.width)
        assertEquals(300f, decoded.height)
    }
}

class LayoutManifestTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testEmptyManifest() {
        val manifest = LayoutManifest()

        assertEquals("default", manifest.layoutName)
        assertTrue(manifest.components.isEmpty())
        assertEquals(1, manifest.version)
    }

    @Test
    fun testManifestSerialization() {
        val manifest = LayoutManifest(
            layoutName = "my-layout",
            components = listOf(
                ComponentInstance(
                    id = "btn-1",
                    name = "Button",
                    properties = mapOf("label" to "OK"),
                    xOffset = 10f,
                    yOffset = 20f,
                ),
                ComponentInstance(
                    id = "txt-1",
                    name = "Text",
                    properties = mapOf("text" to "Hello"),
                    xOffset = 30f,
                    yOffset = 40f,
                ),
            ),
            version = 1,
        )

        val jsonString = json.encodeToString(LayoutManifest.serializer(), manifest)
        val decoded = json.decodeFromString(LayoutManifest.serializer(), jsonString)

        assertEquals(manifest, decoded)
        assertEquals(2, decoded.components.size)
        assertEquals("Button", decoded.components[0].name)
        assertEquals("Text", decoded.components[1].name)
    }

    @Test
    fun testManifestRoundTrip() {
        // Create manifest, serialize, deserialize, verify
        val original = LayoutManifest(
            components = listOf(
                ComponentInstance(id = "1", name = "Button", properties = mapOf("label" to "Click")),
                ComponentInstance(id = "2", name = "TextField", properties = mapOf("label" to "Name")),
                ComponentInstance(id = "3", name = "Card"),
            ),
        )

        val jsonString = json.encodeToString(LayoutManifest.serializer(), original)
        val restored = json.decodeFromString(LayoutManifest.serializer(), jsonString)

        assertEquals(original.components.size, restored.components.size)
        original.components.forEachIndexed { index, expected ->
            assertEquals(expected, restored.components[index])
        }
    }

    @Test
    fun testDeserializeWithUnknownKeys() {
        // Simulate future schema evolution – extra keys should be ignored
        val jsonString = """
        {
            "layoutName": "test",
            "components": [],
            "version": 1,
            "extraField": "should be ignored"
        }
        """.trimIndent()

        val manifest = json.decodeFromString(LayoutManifest.serializer(), jsonString)
        assertEquals("test", manifest.layoutName)
    }
}

class DesignSystemSchemaTest {

    @Test
    fun testComponentsAvailable() {
        assertTrue(DesignSystemSchema.components.isNotEmpty(), "Schema should have components")
        assertTrue(DesignSystemSchema.components.size >= 5, "Should have at least 5 components")
    }

    @Test
    fun testFindComponentByName() {
        val button = DesignSystemSchema.findComponent("Button")
        assertNotNull(button)
        assertEquals("Button", button.name)
        assertEquals("Input", button.category)
    }

    @Test
    fun testFindComponentCaseInsensitive() {
        val button = DesignSystemSchema.findComponent("button")
        assertNotNull(button)
        assertEquals("Button", button.name)
    }

    @Test
    fun testResolveColor() {
        val primary = DesignSystemSchema.resolveColor("Primary")
        assertNotNull(primary)
        assertEquals("#6750A4", primary)
    }

    @Test
    fun testResolveColorFriendlyName() {
        val blue = DesignSystemSchema.resolveColor("Blue")
        assertNotNull(blue)
    }

    @Test
    fun testResolveColorCaseInsensitive() {
        val color = DesignSystemSchema.resolveColor("primary")
        assertNotNull(color)
    }

    @Test
    fun testComponentNames() {
        val names = DesignSystemSchema.componentNames
        assertTrue(names.contains("Button"))
        assertTrue(names.contains("Text"))
        assertTrue(names.contains("TextField"))
        assertTrue(names.contains("Card"))
        assertTrue(names.contains("Checkbox"))
    }

    @Test
    fun testTokensSpacing() {
        val spacing = DesignSystemSchema.tokens.spacing
        assertTrue(spacing.isNotEmpty())
        assertTrue(spacing.contains(8))
        assertTrue(spacing.contains(16))
    }

    @Test
    fun testTokensFontSizes() {
        val fontSizes = DesignSystemSchema.tokens.fontSizes
        assertTrue(fontSizes.containsKey("Body"))
        assertEquals(16, fontSizes["Body"])
    }

    @Test
    fun testComponentDefaultProperties() {
        val button = DesignSystemSchema.findComponent("Button")
        assertNotNull(button)
        assertTrue(button.defaultProperties.containsKey("label"))
        assertTrue(button.editableProperties.contains("label"))
    }
}

