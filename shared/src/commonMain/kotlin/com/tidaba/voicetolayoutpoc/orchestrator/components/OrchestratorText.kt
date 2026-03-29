package com.tidaba.voicetolayoutpoc.orchestrator.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn

/**
 * Orchestrator wrapper for Material 3 Text.
 *
 * Supported properties:
 * - "text"     → content to display (default: "Hello World")
 * - "fontSize" → typography style: "Display", "Headline", "Title", "Body", "Label", "Caption"
 * - "color"    → color name: "Primary", "Secondary", "Error", "OnSurface" (default: OnSurface)
 */
@Composable
fun OrchestratorText(properties: Map<String, String>) {
    val content = properties["text"] ?: "Hello World"

    val style = when (properties["fontSize"]?.lowercase()) {
        "display"  -> MaterialTheme.typography.displayMedium
        "headline" -> MaterialTheme.typography.headlineMedium
        "title"    -> MaterialTheme.typography.titleLarge
        "body"     -> MaterialTheme.typography.bodyLarge
        "label"    -> MaterialTheme.typography.labelLarge
        "caption"  -> MaterialTheme.typography.labelSmall
        else       -> MaterialTheme.typography.bodyLarge
    }

    val color = when (properties["color"]?.lowercase()) {
        "primary"   -> MaterialTheme.colorScheme.primary
        "secondary" -> MaterialTheme.colorScheme.secondary
        "tertiary"  -> MaterialTheme.colorScheme.tertiary
        "error"     -> MaterialTheme.colorScheme.error
        "onsurface" -> MaterialTheme.colorScheme.onSurface
        "outline"   -> MaterialTheme.colorScheme.outline
        else        -> MaterialTheme.colorScheme.onSurface
    }

    Text(
        text = content,
        style = style,
        color = color,
        modifier = Modifier.widthIn(max = 300.dp),
    )
}

