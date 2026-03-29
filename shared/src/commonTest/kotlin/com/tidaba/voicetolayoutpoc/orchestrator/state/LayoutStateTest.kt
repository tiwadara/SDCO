package com.tidaba.voicetolayoutpoc.orchestrator.state

import com.tidaba.voicetolayoutpoc.orchestrator.models.ComponentInstance
import com.tidaba.voicetolayoutpoc.orchestrator.models.LayoutManifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LayoutStateTest {

    // ── addComponent ───────────────────────────────────────────────────

    @Test
    fun testAddComponent() {
        val state = LayoutState()
        val component = ComponentInstance(id = "btn-1", name = "Button")

        state.addComponent(component)

        assertEquals(1, state.manifest.value.components.size)
        assertEquals("Button", state.manifest.value.components.first().name)
    }

    @Test
    fun testAddMultipleComponents() {
        val state = LayoutState()
        state.addComponent(ComponentInstance(id = "1", name = "Button"))
        state.addComponent(ComponentInstance(id = "2", name = "Text"))
        state.addComponent(ComponentInstance(id = "3", name = "Card"))

        assertEquals(3, state.componentCount)
    }

    @Test
    fun testAddDuplicateIdRejected() {
        val state = LayoutState()
        state.addComponent(ComponentInstance(id = "dup", name = "Button"))
        state.addComponent(ComponentInstance(id = "dup", name = "Text"))

        assertEquals(1, state.componentCount)
        assertEquals("Button", state.manifest.value.components.first().name)
    }

    // ── moveComponent ──────────────────────────────────────────────────

    @Test
    fun testMoveComponent() {
        val state = LayoutState()
        state.addComponent(ComponentInstance(id = "m1", name = "Button"))

        state.moveComponent("m1", 100f, 200f)

        val moved = state.manifest.value.components.first()
        assertEquals(100f, moved.xOffset)
        assertEquals(200f, moved.yOffset)
    }

    @Test
    fun testMoveComponentClampsNegative() {
        val state = LayoutState()
        state.addComponent(ComponentInstance(id = "m2", name = "Button"))

        state.moveComponent("m2", -50f, -100f)

        val moved = state.manifest.value.components.first()
        assertEquals(0f, moved.xOffset)
        assertEquals(0f, moved.yOffset)
    }

    @Test
    fun testMoveComponentClampsMax() {
        val state = LayoutState()
        state.addComponent(ComponentInstance(id = "m3", name = "Button"))

        state.moveComponent("m3", 99999f, 99999f)

        val moved = state.manifest.value.components.first()
        assertEquals(LayoutState.MAX_CANVAS_WIDTH, moved.xOffset)
        assertEquals(LayoutState.MAX_CANVAS_HEIGHT, moved.yOffset)
    }

    @Test
    fun testMoveComponentIgnoresNaN() {
        val state = LayoutState()
        state.addComponent(ComponentInstance(id = "m4", name = "Button", xOffset = 10f, yOffset = 20f))

        state.moveComponent("m4", Float.NaN, 50f)

        // Should remain unchanged
        val c = state.manifest.value.components.first()
        assertEquals(10f, c.xOffset)
        assertEquals(20f, c.yOffset)
    }

    @Test
    fun testMoveComponentIgnoresInfinity() {
        val state = LayoutState()
        state.addComponent(ComponentInstance(id = "m5", name = "Button", xOffset = 10f, yOffset = 20f))

        state.moveComponent("m5", Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)

        val c = state.manifest.value.components.first()
        assertEquals(10f, c.xOffset)
        assertEquals(20f, c.yOffset)
    }

    @Test
    fun testMoveNonExistentComponentDoesNothing() {
        val state = LayoutState()
        state.addComponent(ComponentInstance(id = "exists", name = "Button", xOffset = 5f))

        state.moveComponent("ghost", 100f, 100f)

        assertEquals(1, state.componentCount)
        assertEquals(5f, state.manifest.value.components.first().xOffset)
    }

    // ── removeComponent ────────────────────────────────────────────────

    @Test
    fun testRemoveComponent() {
        val state = LayoutState()
        state.addComponent(ComponentInstance(id = "r1", name = "Button"))
        state.addComponent(ComponentInstance(id = "r2", name = "Text"))

        state.removeComponent("r1")

        assertEquals(1, state.componentCount)
        assertEquals("Text", state.manifest.value.components.first().name)
    }

    @Test
    fun testRemoveNonExistentDoesNothing() {
        val state = LayoutState()
        state.addComponent(ComponentInstance(id = "r3", name = "Button"))

        state.removeComponent("nope")

        assertEquals(1, state.componentCount)
    }

    // ── updateComponentProperties ──────────────────────────────────────

    @Test
    fun testUpdateComponentProperties() {
        val state = LayoutState()
        state.addComponent(
            ComponentInstance(id = "p1", name = "Button", properties = mapOf("label" to "OK"))
        )

        state.updateComponentProperties("p1", mapOf("label" to "Cancel", "color" to "Red"))

        val props = state.manifest.value.components.first().properties
        assertEquals("Cancel", props["label"])
        assertEquals("Red", props["color"])
    }

    @Test
    fun testUpdatePropertiesMerges() {
        val state = LayoutState()
        state.addComponent(
            ComponentInstance(id = "p2", name = "Button", properties = mapOf("label" to "OK", "color" to "Blue"))
        )

        state.updateComponentProperties("p2", mapOf("color" to "Red"))

        val props = state.manifest.value.components.first().properties
        assertEquals("OK", props["label"]) // untouched
        assertEquals("Red", props["color"]) // overwritten
    }

    // ── loadManifest / clearAll ────────────────────────────────────────

    @Test
    fun testLoadManifest() {
        val state = LayoutState()
        val manifest = LayoutManifest(
            layoutName = "loaded",
            components = listOf(
                ComponentInstance(id = "l1", name = "Card"),
                ComponentInstance(id = "l2", name = "Text"),
            ),
        )

        state.loadManifest(manifest)

        assertEquals("loaded", state.manifest.value.layoutName)
        assertEquals(2, state.componentCount)
    }

    @Test
    fun testClearAll() {
        val state = LayoutState()
        state.addComponent(ComponentInstance(id = "c1", name = "Button"))
        state.addComponent(ComponentInstance(id = "c2", name = "Text"))

        state.clearAll()

        assertEquals(0, state.componentCount)
        assertEquals("default", state.manifest.value.layoutName)
    }

    // ── getComponentById ───────────────────────────────────────────────

    @Test
    fun testGetComponentById() {
        val state = LayoutState()
        state.addComponent(ComponentInstance(id = "g1", name = "Switch"))

        assertNotNull(state.getComponentById("g1"))
        assertEquals("Switch", state.getComponentById("g1")?.name)
    }

    @Test
    fun testGetComponentByIdReturnsNull() {
        val state = LayoutState()
        assertNull(state.getComponentById("missing"))
    }
}

