package com.tidaba.voicetolayoutpoc.orchestrator.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Orchestrator wrapper for Material 3 TextField.
 *
 * Supported properties:
 * - "label"       → field label above/inside the field (default: "Enter text")
 * - "placeholder" → hint text shown when empty
 * - "style"       → "Outlined" (default) or "Filled"
 *
 * Known limitation (POC): typed text state is local and not persisted.
 */
@Composable
fun OrchestratorTextField(properties: Map<String, String>) {
    val label = properties["label"] ?: "Enter text"
    val placeholder = properties["placeholder"]
    val style = properties["style"]?.lowercase() ?: "outlined"

    var text by remember { mutableStateOf("") }

    val modifier = Modifier.defaultMinSize(minWidth = 200.dp, minHeight = 56.dp)

    val labelContent: @Composable (() -> Unit) = { Text(label) }
    val placeholderContent: @Composable (() -> Unit)? =
        if (!placeholder.isNullOrBlank()) {
            { Text(placeholder) }
        } else null

    when (style) {
        "filled" -> TextField(
            value = text,
            onValueChange = { text = it },
            label = labelContent,
            placeholder = placeholderContent,
            modifier = modifier,
            singleLine = true,
        )

        else -> OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = labelContent,
            placeholder = placeholderContent,
            modifier = modifier,
            singleLine = true,
        )
    }
}

