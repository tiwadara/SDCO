package com.tidaba.voicetolayoutpoc.orchestrator

import com.tidaba.voicetolayoutpoc.orchestrator.models.ComponentDefinition
import com.tidaba.voicetolayoutpoc.orchestrator.models.DesignSystemTokens

/**
 * Static schema defining the available Material 3 components and design tokens.
 * This serves as the "catalog" that voice commands and the canvas reference.
 *
 * Can be migrated to a remote JSON file in the future for dynamic updates.
 */
object DesignSystemSchema {

    // ── Available Components ───────────────────────────────────────────

    val components: List<ComponentDefinition> = listOf(
        ComponentDefinition(
            name = "Button",
            category = "Input",
            defaultProperties = mapOf(
                "label" to "Button",
                "color" to "Primary",
                "style" to "Filled", // Filled, Outlined, Text, Tonal
            ),
            editableProperties = listOf("label", "color", "style"),
        ),
        ComponentDefinition(
            name = "Text",
            category = "Display",
            defaultProperties = mapOf(
                "text" to "Hello World",
                "fontSize" to "Body",
                "color" to "OnSurface",
            ),
            editableProperties = listOf("text", "fontSize", "color"),
        ),
        ComponentDefinition(
            name = "TextField",
            category = "Input",
            defaultProperties = mapOf(
                "label" to "Enter text",
                "placeholder" to "",
                "style" to "Outlined", // Outlined, Filled
            ),
            editableProperties = listOf("label", "placeholder", "style"),
        ),
        ComponentDefinition(
            name = "Card",
            category = "Layout",
            defaultProperties = mapOf(
                "elevation" to "1",
                "cornerRadius" to "12",
            ),
            editableProperties = listOf("elevation", "cornerRadius"),
        ),
        ComponentDefinition(
            name = "Checkbox",
            category = "Input",
            defaultProperties = mapOf(
                "checked" to "false",
                "label" to "Option",
            ),
            editableProperties = listOf("checked", "label"),
        ),
        ComponentDefinition(
            name = "Switch",
            category = "Input",
            defaultProperties = mapOf(
                "checked" to "false",
                "label" to "Toggle",
            ),
            editableProperties = listOf("checked", "label"),
        ),
        ComponentDefinition(
            name = "Image",
            category = "Display",
            defaultProperties = mapOf(
                "placeholder" to "true",
                "contentDescription" to "Image",
            ),
            editableProperties = listOf("contentDescription"),
        ),
    )

    // ── Design Tokens ──────────────────────────────────────────────────

    val tokens: DesignSystemTokens = DesignSystemTokens(
        colors = mapOf(
            // Material 3 semantic names → hex fallback values
            "Primary" to "#6750A4",
            "OnPrimary" to "#FFFFFF",
            "Secondary" to "#625B71",
            "OnSecondary" to "#FFFFFF",
            "Tertiary" to "#7D5260",
            "Error" to "#B3261E",
            "Background" to "#FFFBFE",
            "Surface" to "#FFFBFE",
            "OnSurface" to "#1C1B1F",
            "Outline" to "#79747E",
            // Friendly aliases (mapped to the same values)
            "Blue" to "#6750A4",
            "Red" to "#B3261E",
            "Green" to "#386A20",
            "White" to "#FFFFFF",
            "Black" to "#1C1B1F",
            "Gray" to "#79747E",
        ),
        spacing = listOf(4, 8, 12, 16, 24, 32, 48, 64),
        fontSizes = mapOf(
            "Display" to 57,
            "Headline" to 32,
            "Title" to 22,
            "Body" to 16,
            "Label" to 14,
            "Caption" to 12,
        ),
        cornerRadii = listOf(0, 4, 8, 12, 16, 28),
    )

    // ── Helper Functions ───────────────────────────────────────────────

    /**
     * Find a component definition by name (case-insensitive).
     */
    fun findComponent(name: String): ComponentDefinition? {
        return components.find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Resolve a color name to its hex value.
     * Supports both Material names ("Primary") and friendly names ("Blue").
     */
    fun resolveColor(name: String): String? {
        return tokens.colors.entries.find { it.key.equals(name, ignoreCase = true) }?.value
    }

    /**
     * Get all component names as a list (useful for voice command matching).
     */
    val componentNames: List<String>
        get() = components.map { it.name }
}

