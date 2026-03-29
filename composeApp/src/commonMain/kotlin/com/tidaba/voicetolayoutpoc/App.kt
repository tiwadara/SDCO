package com.tidaba.voicetolayoutpoc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tidaba.voicetolayoutpoc.orchestrator.Orchestrator
import com.tidaba.voicetolayoutpoc.orchestrator.canvas.OrchestratorCanvas
import com.tidaba.voicetolayoutpoc.orchestrator.llm.GeminiClient
import com.tidaba.voicetolayoutpoc.orchestrator.models.ComponentInstance
import com.tidaba.voicetolayoutpoc.orchestrator.storage.LayoutStorage
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun App() {
    MaterialTheme {
        // ── API Key gate ───────────────────────────────────────
        var apiKey by remember { mutableStateOf("") }
        var selectedModel by remember { mutableStateOf("gemini-1.5-flash") }
        var orchestrator by remember { mutableStateOf<Orchestrator?>(null) }

        if (orchestrator == null) {
            ApiKeyScreen(
                apiKey = apiKey,
                onApiKeyChange = { apiKey = it },
                selectedModel = selectedModel,
                onModelChange = { selectedModel = it },
                onConfirm = {
                    orchestrator = Orchestrator(
                        geminiApiKey = apiKey,
                        model = selectedModel,
                        storage = LayoutStorage(),
                    )
                },
            )
        } else {
            OrchestratorScreen(
                orchestrator = orchestrator!!,
                onChangeApiKey = {
                    orchestrator?.close()
                    orchestrator = null
                },
            )
        }
    }
}

// ── API Key Entry ──────────────────────────────────────────────────────

@Composable
private fun ApiKeyScreen(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    selectedModel: String,
    onModelChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var modelDropdownExpanded by remember { mutableStateOf(false) }
    var availableModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var isFetchingModels by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🎙️ Voice-to-Layout POC", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Enter your Gemini API key to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))

        // API Key field
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    onApiKeyChange(it)
                    // Reset models when key changes
                    availableModels = emptyList()
                    fetchError = null
                },
                label = { Text("Gemini API Key") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    scope.launch {
                        isFetchingModels = true
                        fetchError = null
                        GeminiClient.fetchAvailableModels(apiKey)
                            .onSuccess { models ->
                                availableModels = models
                                if (models.isNotEmpty() && selectedModel !in models) {
                                    // Auto-select first flash model or first model
                                    val flash = models.firstOrNull { it.contains("flash") }
                                    onModelChange(flash ?: models.first())
                                }
                            }
                            .onFailure { error ->
                                fetchError = error.message?.take(200)
                            }
                        isFetchingModels = false
                    }
                },
                enabled = apiKey.isNotBlank() && !isFetchingModels,
            ) {
                if (isFetchingModels) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(18.dp).height(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Fetch Models")
                }
            }
        }

        // Error message
        if (fetchError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "❌ $fetchError",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // Model selector (shown after fetching)
        if (availableModels.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${availableModels.size} models available",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth(0.8f)) {
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = {},
                    label = { Text("Model") },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Transparent overlay that captures clicks to open dropdown
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { modelDropdownExpanded = true },
                )
                DropdownMenu(
                    expanded = modelDropdownExpanded,
                    onDismissRequest = { modelDropdownExpanded = false },
                    modifier = Modifier.heightIn(max = 400.dp),
                ) {
                    availableModels.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onModelChange(model)
                                modelDropdownExpanded = false
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onConfirm,
            enabled = apiKey.isNotBlank() && availableModels.isNotEmpty(),
        ) {
            Text("Start Orchestrator")
        }
    }
}

// ── Main Orchestrator Screen ───────────────────────────────────────────

@Composable
private fun OrchestratorScreen(orchestrator: Orchestrator, onChangeApiKey: () -> Unit) {
    val manifest by orchestrator.layoutState.manifest.collectAsState()
    val isLoading by orchestrator.isLoading.collectAsState()
    val scope = rememberCoroutineScope()

    var userInput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Command Input Bar ──────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "🎨 Orchestrator (${manifest.components.size} components)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = onChangeApiKey) {
                    Text("🔑 Change Key", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text("e.g. \"Add a blue button labeled Submit\"") },
                    singleLine = true,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            statusMessage = null
                            orchestrator.processCommand(userInput)
                                .onSuccess { count ->
                                    statusMessage = "✅ Added $count component(s)"
                                    userInput = ""
                                }
                                .onFailure { error ->
                                    val cause = error.cause?.message?.let { " | Cause: $it" } ?: ""
                                    statusMessage = "❌ ${error.message}$cause".take(300)
                                }
                            isProcessing = false
                        }
                    },
                    enabled = userInput.isNotBlank() && !isProcessing,
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(18.dp).height(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Add")
                    }
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            orchestrator.clearAndSave()
                            statusMessage = "🗑 Canvas cleared"
                        }
                    },
                ) {
                    Text("Clear")
                }
            }

            // Status message
            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusMessage!!.startsWith("❌"))
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
            }

            // ── Quick-add buttons ──────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                QuickAdd("+ Button") {
                    orchestrator.layoutState.addComponent(
                        ComponentInstance(
                            name = "Button",
                            properties = mapOf("label" to "Btn ${Random.nextInt(100)}"),
                            xOffset = Random.nextFloat() * 300 + 20,
                            yOffset = Random.nextFloat() * 400 + 20,
                        )
                    )
                }
                QuickAdd("+ Text") {
                    orchestrator.layoutState.addComponent(
                        ComponentInstance(
                            name = "Text",
                            properties = mapOf("text" to "Hello #${Random.nextInt(100)}"),
                            xOffset = Random.nextFloat() * 300 + 20,
                            yOffset = Random.nextFloat() * 400 + 20,
                        )
                    )
                }
                QuickAdd("+ Card") {
                    orchestrator.layoutState.addComponent(
                        ComponentInstance(
                            name = "Card",
                            properties = mapOf("title" to "Card ${Random.nextInt(100)}", "subtitle" to "Drag me"),
                            xOffset = Random.nextFloat() * 250 + 20,
                            yOffset = Random.nextFloat() * 350 + 20,
                        )
                    )
                }
                QuickAdd("+ TextField") {
                    orchestrator.layoutState.addComponent(
                        ComponentInstance(
                            name = "TextField",
                            properties = mapOf("label" to "Input ${Random.nextInt(100)}"),
                            xOffset = Random.nextFloat() * 250 + 20,
                            yOffset = Random.nextFloat() * 400 + 20,
                        )
                    )
                }
                QuickAdd("+ Checkbox") {
                    orchestrator.layoutState.addComponent(
                        ComponentInstance(
                            name = "Checkbox",
                            properties = mapOf("label" to "Option ${Random.nextInt(100)}"),
                            xOffset = Random.nextFloat() * 300 + 20,
                            yOffset = Random.nextFloat() * 400 + 20,
                        )
                    )
                }
                QuickAdd("+ Switch") {
                    orchestrator.layoutState.addComponent(
                        ComponentInstance(
                            name = "Switch",
                            properties = mapOf("label" to "Toggle ${Random.nextInt(100)}"),
                            xOffset = Random.nextFloat() * 300 + 20,
                            yOffset = Random.nextFloat() * 400 + 20,
                        )
                    )
                }
                QuickAdd("+ Image") {
                    orchestrator.layoutState.addComponent(
                        ComponentInstance(
                            name = "Image",
                            properties = mapOf("contentDescription" to "Photo"),
                            xOffset = Random.nextFloat() * 300 + 20,
                            yOffset = Random.nextFloat() * 400 + 20,
                        )
                    )
                }
            }
        }

        HorizontalDivider()

        // ── Canvas ─────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            OrchestratorCanvas(
                manifest = manifest,
                onComponentMoved = { id, x, y ->
                    orchestrator.layoutState.moveComponent(id, x, y)
                },
                onComponentDeleted = { id ->
                    scope.launch {
                        orchestrator.deleteComponent(id)
                        statusMessage = "🗑 Component deleted"
                    }
                },
            )

            // Loading overlay while restoring persisted layout
            if (isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Restoring layout…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAdd(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}