package com.tidaba.voicetolayoutpoc.orchestrator.canvas

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tidaba.voicetolayoutpoc.orchestrator.models.ComponentInstance
import com.tidaba.voicetolayoutpoc.orchestrator.models.LayoutManifest
import com.tidaba.voicetolayoutpoc.orchestrator.registry.ComponentRegistry
import kotlin.math.roundToInt

/**
 * The main interactive canvas that renders all components at their
 * stored positions and supports drag-and-drop repositioning.
 *
 * @param manifest   Current layout state (list of components + positions)
 * @param onComponentMoved Callback when a component is dragged to a new position
 */
@Composable
fun OrchestratorCanvas(
    manifest: LayoutManifest,
    onComponentMoved: (id: String, newX: Float, newY: Float) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        manifest.components.forEach { component ->
            DraggableComponent(
                component = component,
                onMoved = onComponentMoved,
            )
        }
    }
}

/**
 * Wraps a single component with offset positioning and drag gesture handling.
 *
 * Uses a local [Offset] accumulator during drag so visual movement is
 * instantaneous, then commits the final position via [onMoved] on drag end.
 * This avoids a StateFlow update on every pointer-move event.
 */
@Composable
private fun DraggableComponent(
    component: ComponentInstance,
    onMoved: (id: String, newX: Float, newY: Float) -> Unit,
) {
    // Local drag delta — accumulates during a drag gesture
    var localDragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    val borderColor = if (isDragging) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (component.xOffset + localDragOffset.x).roundToInt(),
                    (component.yOffset + localDragOffset.y).roundToInt(),
                )
            }
            .border(
                width = if (isDragging) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(4.dp),
            )
            .pointerInput(component.id) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        localDragOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        localDragOffset += dragAmount
                    },
                    onDragEnd = {
                        isDragging = false
                        onMoved(
                            component.id,
                            component.xOffset + localDragOffset.x,
                            component.yOffset + localDragOffset.y,
                        )
                        localDragOffset = Offset.Zero
                    },
                    onDragCancel = {
                        isDragging = false
                        localDragOffset = Offset.Zero
                    },
                )
            },
    ) {
        ComponentRegistry.render(component)
    }
}

