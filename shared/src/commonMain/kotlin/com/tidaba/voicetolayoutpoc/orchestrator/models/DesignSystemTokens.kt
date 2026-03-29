package com.tidaba.voicetolayoutpoc.orchestrator.models

import kotlinx.serialization.Serializable

/**
 * Describes a component type available in the design system.
 *
 * @property name Display name (e.g., "Button")
 * @property category Grouping category (e.g., "Input", "Display", "Layout")
 * @property defaultProperties Default property values when the component is first created
 * @property editableProperties List of property keys that can be modified by the user/voice
 */
@Serializable
data class ComponentDefinition(
    val name: String,
    val category: String,
    val defaultProperties: Map<String, String> = emptyMap(),
    val editableProperties: List<String> = emptyList(),
)

/**
 * Design system tokens: colors, spacing, typography scales, etc.
 *
 * @property colors Available color tokens (both Material names and friendly aliases)
 * @property spacing Standard spacing scale in dp
 * @property fontSizes Available font size presets
 * @property cornerRadii Available corner radius presets
 */
@Serializable
data class DesignSystemTokens(
    val colors: Map<String, String> = emptyMap(),
    val spacing: List<Int> = emptyList(),
    val fontSizes: Map<String, Int> = emptyMap(),
    val cornerRadii: List<Int> = emptyList(),
)

