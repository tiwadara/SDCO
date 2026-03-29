package com.tidaba.voicetolayoutpoc.orchestrator.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Orchestrator wrapper for Material 3 Button.
 *
 * Supported properties:
 * - "label" → button text (default: "Button")
 * - "color" → color name: "Primary", "Secondary", "Error", "Blue", "Red", "Green"
 * - "style" → button variant: "Filled" (default), "Outlined", "Text", "Tonal"
 */
@Composable
fun OrchestratorButton(properties: Map<String, String>) {
    val label = properties["label"] ?: "Button"
    val colorName = properties["color"]?.lowercase()
    val style = properties["style"]?.lowercase() ?: "filled"

    // Resolve container color from friendly/semantic name
    val containerColor = when (colorName) {
        "primary", "blue"      -> MaterialTheme.colorScheme.primary
        "secondary", "gray"    -> MaterialTheme.colorScheme.secondary
        "tertiary"             -> MaterialTheme.colorScheme.tertiary
        "error", "red"         -> MaterialTheme.colorScheme.error
        "green"                -> MaterialTheme.colorScheme.tertiary // closest M3 mapping
        else                   -> MaterialTheme.colorScheme.primary
    }

    val contentColor = when (colorName) {
        "primary", "blue"      -> MaterialTheme.colorScheme.onPrimary
        "secondary", "gray"    -> MaterialTheme.colorScheme.onSecondary
        "tertiary"             -> MaterialTheme.colorScheme.onTertiary
        "error", "red"         -> MaterialTheme.colorScheme.onError
        "green"                -> MaterialTheme.colorScheme.onTertiary
        else                   -> MaterialTheme.colorScheme.onPrimary
    }

    val modifier = Modifier.defaultMinSize(minWidth = 120.dp, minHeight = 48.dp)

    when (style) {
        "outlined" -> OutlinedButton(
            onClick = { /* No-op for POC */ },
            modifier = modifier,
        ) {
            Text(label)
        }

        "text" -> TextButton(
            onClick = { /* No-op for POC */ },
            modifier = modifier,
        ) {
            Text(label)
        }

        "tonal" -> FilledTonalButton(
            onClick = { /* No-op for POC */ },
            modifier = modifier,
        ) {
            Text(label)
        }

        else -> Button(
            onClick = { /* No-op for POC */ },
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        ) {
            Text(label)
        }
    }
}

