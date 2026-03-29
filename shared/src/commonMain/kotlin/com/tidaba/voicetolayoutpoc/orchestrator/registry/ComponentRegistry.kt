package com.tidaba.voicetolayoutpoc.orchestrator.registry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tidaba.voicetolayoutpoc.orchestrator.DesignSystemSchema
import com.tidaba.voicetolayoutpoc.orchestrator.components.OrchestratorButton
import com.tidaba.voicetolayoutpoc.orchestrator.components.OrchestratorCard
import com.tidaba.voicetolayoutpoc.orchestrator.components.OrchestratorCheckbox
import com.tidaba.voicetolayoutpoc.orchestrator.components.OrchestratorImage
import com.tidaba.voicetolayoutpoc.orchestrator.components.OrchestratorSwitch
import com.tidaba.voicetolayoutpoc.orchestrator.components.OrchestratorText
import com.tidaba.voicetolayoutpoc.orchestrator.components.OrchestratorTextField
import com.tidaba.voicetolayoutpoc.orchestrator.models.ComponentInstance

/**
 * Central registry that maps component names to their Composable implementations.
 *
 * This is the single entry point Phase 3 canvas uses:
 * ```
 * ComponentRegistry.render(component)
 * ```
 */
object ComponentRegistry {

    /**
     * Render a [ComponentInstance] by looking up its name in the registry.
     * Uses case-insensitive matching to be forgiving of LLM output casing.
     * Falls back to an error placeholder for unrecognised component names.
     */
    @Composable
    fun render(component: ComponentInstance) {
        when (component.name.lowercase()) {
            "button"    -> OrchestratorButton(component.properties)
            "text"      -> OrchestratorText(component.properties)
            "textfield" -> OrchestratorTextField(component.properties)
            "card"      -> OrchestratorCard(component.properties)
            "checkbox"  -> OrchestratorCheckbox(component.properties)
            "switch"    -> OrchestratorSwitch(component.properties)
            "image"     -> OrchestratorImage(component.properties)
            else        -> UnknownComponent(component.name)
        }
    }

    /**
     * List of all component names the registry can render.
     */
    fun getAvailableComponents(): List<String> =
        listOf("Button", "Text", "TextField", "Card", "Checkbox", "Switch", "Image")

    /**
     * Validate that every component defined in [DesignSystemSchema] has a
     * matching entry in this registry. Useful as a build/test sanity check.
     *
     * @return A pair of (matched, unmatched) component names.
     */
    fun validate(): Pair<List<String>, List<String>> {
        val registered = getAvailableComponents().map { it.lowercase() }.toSet()
        val schema = DesignSystemSchema.componentNames

        val matched = schema.filter { it.lowercase() in registered }
        val unmatched = schema.filter { it.lowercase() !in registered }
        return matched to unmatched
    }
}

/**
 * Fallback composable rendered when a component name is not in the registry.
 */
@Composable
private fun UnknownComponent(name: String) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(
            text = "⚠️ Unknown: $name",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

