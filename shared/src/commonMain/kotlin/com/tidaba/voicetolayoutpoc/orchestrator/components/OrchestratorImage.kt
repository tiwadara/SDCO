package com.tidaba.voicetolayoutpoc.orchestrator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Orchestrator wrapper for a placeholder Image component.
 *
 * Supported properties:
 * - "contentDescription" → accessibility label (default: "Image")
 *
 * Since we can't load remote images in the POC, this renders a styled placeholder box.
 */
@Composable
fun OrchestratorImage(properties: Map<String, String>) {
    val contentDescription = properties["contentDescription"] ?: "Image"

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(120.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            text = contentDescription,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

