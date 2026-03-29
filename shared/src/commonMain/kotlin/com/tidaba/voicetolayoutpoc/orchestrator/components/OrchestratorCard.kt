package com.tidaba.voicetolayoutpoc.orchestrator.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Orchestrator wrapper for Material 3 ElevatedCard.
 *
 * Supported properties:
 * - "title"    → main heading (default: "Card Title")
 * - "subtitle" → secondary text (default: "Subtitle")
 * - "content"  → optional body text (default: empty)
 */
@Composable
fun OrchestratorCard(properties: Map<String, String>) {
    val title = properties["title"] ?: "Card Title"
    val subtitle = properties["subtitle"] ?: "Subtitle"
    val content = properties["content"]

    ElevatedCard(
        modifier = Modifier
            .defaultMinSize(minWidth = 200.dp, minHeight = 80.dp)
            .width(240.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

