package com.tidaba.voicetolayoutpoc.orchestrator.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Orchestrator wrapper for Material 3 Switch with a label.
 *
 * Supported properties:
 * - "label"   → text next to switch (default: "Toggle")
 * - "checked" → initial state: "true" or "false" (default: "false")
 *
 * Known limitation (POC): checked state is local and not persisted.
 */
@Composable
fun OrchestratorSwitch(properties: Map<String, String>) {
    val label = properties["label"] ?: "Toggle"
    val initialChecked = properties["checked"]?.lowercase() == "true"

    var checked by remember { mutableStateOf(initialChecked) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .defaultMinSize(minWidth = 120.dp, minHeight = 48.dp)
            .clickable { checked = !checked },
    ) {
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

