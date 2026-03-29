package com.tidaba.voicetolayoutpoc.orchestrator.models

import kotlinx.serialization.Serializable

/**
 * The "save file" format: contains all component instances in a layout.
 *
 * @property layoutName Name identifier for this layout
 * @property components List of all UI component instances on the canvas
 * @property version Schema version for future migration support
 */
@Serializable
data class LayoutManifest(
    val layoutName: String = "default",
    val components: List<ComponentInstance> = emptyList(),
    val version: Int = 1,
)

