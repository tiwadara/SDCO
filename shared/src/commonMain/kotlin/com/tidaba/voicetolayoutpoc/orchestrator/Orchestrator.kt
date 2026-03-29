package com.tidaba.voicetolayoutpoc.orchestrator

import com.tidaba.voicetolayoutpoc.orchestrator.llm.GeminiClient
import com.tidaba.voicetolayoutpoc.orchestrator.llm.IntentParser
import com.tidaba.voicetolayoutpoc.orchestrator.state.LayoutState
import com.tidaba.voicetolayoutpoc.orchestrator.storage.LayoutStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Top-level facade that wires together the entire orchestrator pipeline.
 */
class Orchestrator(
    geminiApiKey: String,
    model: String = "gemini-1.5-flash",
    private val storage: LayoutStorage,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val geminiClient = GeminiClient(geminiApiKey, model)
    private val intentParser = IntentParser(geminiClient)

    /** Observable layout state — drive the canvas UI from this. */
    val layoutState = LayoutState()

    private val _isLoading = MutableStateFlow(true)
    /** True while the persisted layout is being restored. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        scope.launch {
            storage.load().onSuccess { manifest ->
                if (manifest.components.isNotEmpty()) {
                    layoutState.loadManifest(manifest)
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Process a natural-language command.
     * Sends it to the LLM, parses the response, adds components to the
     * canvas, and persists the updated layout.
     */
    suspend fun processCommand(userInput: String): Result<Int> {
        return intentParser.parseIntent(userInput)
            .mapCatching { components ->
                components.forEach { layoutState.addComponent(it) }
                save()
                components.size
            }
    }

    /** Delete a single component and persist. */
    suspend fun deleteComponent(id: String) {
        layoutState.removeComponent(id)
        save()
    }

    /** Persist current layout to storage. */
    suspend fun save() {
        storage.save(layoutState.manifest.value)
    }

    /** Clear the canvas and persist the empty state. */
    suspend fun clearAndSave() {
        layoutState.clearAll()
        storage.save(layoutState.manifest.value)
    }

    fun close() {
        geminiClient.close()
    }
}

