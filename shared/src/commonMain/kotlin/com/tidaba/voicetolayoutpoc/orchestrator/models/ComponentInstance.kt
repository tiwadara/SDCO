package com.tidaba.voicetolayoutpoc.orchestrator.models

import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Represents a single UI component instance placed on the canvas.
 *
 * @property id Unique identifier (random hex to avoid collisions)
 * @property name The component type name (e.g., "Button", "Card", "TextField")
 * @property properties Flexible key-value map for component-specific configuration
 * @property xOffset Horizontal position on canvas (Float for smooth dragging)
 * @property yOffset Vertical position on canvas (Float for smooth dragging)
 * @property width Optional width override (null = use default/intrinsic size)
 * @property height Optional height override (null = use default/intrinsic size)
 */
@Serializable
data class ComponentInstance(
    val id: String = generateId(),
    val name: String,
    val properties: Map<String, String> = emptyMap(),
    val xOffset: Float = 0f,
    val yOffset: Float = 0f,
    val width: Float? = null,
    val height: Float? = null,
)

/**
 * Generate a unique ID using random longs encoded as hex.
 * Produces a 32-char hex string with extremely low collision probability.
 */
private fun generateId(): String {
    val hi = Random.nextLong().toULong().toString(16).padStart(16, '0')
    val lo = Random.nextLong().toULong().toString(16).padStart(16, '0')
    return "$hi$lo"
}


