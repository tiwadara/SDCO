package com.tidaba.voicetolayoutpoc.orchestrator.state

import com.tidaba.voicetolayoutpoc.orchestrator.models.ComponentInstance
import com.tidaba.voicetolayoutpoc.orchestrator.models.LayoutManifest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Central state manager for the orchestrator canvas.
 *
 * Exposes an immutable [StateFlow] of [LayoutManifest] that drives the
 * Compose UI. All mutations produce new immutable copies so that
 * StateFlow observers are always notified.
 */
class LayoutState {

    private val _manifest = MutableStateFlow(LayoutManifest())

    /** Observable layout state — collect with `collectAsState()` in Compose. */
    val manifest: StateFlow<LayoutManifest> = _manifest.asStateFlow()

    // ── Canvas Bounds ──────────────────────────────────────────────────

    companion object {
        /** Maximum horizontal extent of the canvas (pixels). */
        const val MAX_CANVAS_WIDTH = 2000f
        /** Maximum vertical extent of the canvas (pixels). */
        const val MAX_CANVAS_HEIGHT = 3000f
    }

    // ── Mutations ──────────────────────────────────────────────────────

    /**
     * Add a new component to the canvas.
     * Duplicate IDs are silently rejected.
     */
    fun addComponent(component: ComponentInstance) {
        _manifest.update { current ->
            if (current.components.any { it.id == component.id }) {
                current // reject duplicate
            } else {
                current.copy(components = current.components + component)
            }
        }
    }

    /**
     * Move a component to a new position, clamped to canvas bounds.
     * Does nothing when [id] is not found or coordinates are non-finite.
     */
    fun moveComponent(id: String, x: Float, y: Float) {
        if (!x.isFinite() || !y.isFinite()) return

        val boundedX = x.coerceIn(0f, MAX_CANVAS_WIDTH)
        val boundedY = y.coerceIn(0f, MAX_CANVAS_HEIGHT)

        _manifest.update { current ->
            current.copy(
                components = current.components.map {
                    if (it.id == id) it.copy(xOffset = boundedX, yOffset = boundedY) else it
                }
            )
        }
    }

    /**
     * Remove a component by its [id].
     */
    fun removeComponent(id: String) {
        _manifest.update { current ->
            current.copy(components = current.components.filter { it.id != id })
        }
    }

    /**
     * Update one or more properties on an existing component.
     * New keys are added; existing keys are overwritten.
     */
    fun updateComponentProperties(id: String, newProps: Map<String, String>) {
        _manifest.update { current ->
            current.copy(
                components = current.components.map {
                    if (it.id == id) it.copy(properties = it.properties + newProps) else it
                }
            )
        }
    }

    /**
     * Replace the entire manifest (e.g. after loading from storage).
     */
    fun loadManifest(manifest: LayoutManifest) {
        _manifest.value = manifest
    }

    /**
     * Reset to an empty canvas.
     */
    fun clearAll() {
        _manifest.value = LayoutManifest()
    }

    // ── Queries ────────────────────────────────────────────────────────

    /** Snapshot lookup — returns null when not found. */
    fun getComponentById(id: String): ComponentInstance? =
        _manifest.value.components.find { it.id == id }

    /** Current number of components on the canvas. */
    val componentCount: Int
        get() = _manifest.value.components.size
}

