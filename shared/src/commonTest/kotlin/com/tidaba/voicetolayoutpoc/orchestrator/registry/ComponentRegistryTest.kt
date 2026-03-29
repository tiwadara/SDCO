package com.tidaba.voicetolayoutpoc.orchestrator.registry

import com.tidaba.voicetolayoutpoc.orchestrator.DesignSystemSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComponentRegistryTest {

    @Test
    fun testAvailableComponentsNotEmpty() {
        val available = ComponentRegistry.getAvailableComponents()
        assertTrue(available.isNotEmpty(), "Registry should have components")
    }

    @Test
    fun testAvailableComponentsContainsCore() {
        val available = ComponentRegistry.getAvailableComponents()
        assertTrue(available.contains("Button"), "Should contain Button")
        assertTrue(available.contains("Text"), "Should contain Text")
        assertTrue(available.contains("TextField"), "Should contain TextField")
        assertTrue(available.contains("Card"), "Should contain Card")
        assertTrue(available.contains("Checkbox"), "Should contain Checkbox")
        assertTrue(available.contains("Switch"), "Should contain Switch")
        assertTrue(available.contains("Image"), "Should contain Image")
    }

    @Test
    fun testRegistryCoversDesignSystemSchema() {
        val (matched, unmatched) = ComponentRegistry.validate()
        assertTrue(
            unmatched.isEmpty(),
            "All schema components should be registered. Unmatched: $unmatched"
        )
        assertEquals(
            DesignSystemSchema.componentNames.size,
            matched.size,
            "Matched count should equal schema count"
        )
    }

    @Test
    fun testAvailableComponentCountMatchesSchema() {
        val registryCount = ComponentRegistry.getAvailableComponents().size
        val schemaCount = DesignSystemSchema.componentNames.size
        assertEquals(
            schemaCount,
            registryCount,
            "Registry should have same count as schema ($schemaCount), got $registryCount"
        )
    }
}

