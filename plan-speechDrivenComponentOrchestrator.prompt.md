# Plan: Speech-Driven Component Orchestrator (SDCO) for KMP

Build a Kotlin Multiplatform module in the `shared` directory that interprets natural language (text input) to dynamically instantiate, position, and persist Compose Multiplatform components. The system uses a registry-based approach (no runtime code generation) with local-first state management and LLM-powered intent parsing.

## Overview

**Input:** User types "Add a blue button at the top"  
**Process:** Text → Gemini LLM → JSON → ComponentInstance → Canvas  
**Output:** Material 3 button appears on draggable canvas, persisted locally  

## Core Principles

- **No Code Generation:** Use registry pattern to map names to existing Composables
- **Platform Agnostic:** All logic in `commonMain` (runs on iOS, Android, Desktop, Web)
- **Local-First:** State is source of truth, persisted as JSON
- **Material 3 as Design System:** Use existing Material components to avoid building custom UI

## Phase 1: Foundation (Data & Storage)

### Step 1.1: Dependencies Already Added ✓
- kotlinx-serialization-json
- kotlinx-coroutines-core
- Ktor client (for Gemini API)
- kotlinx-datetime
- Compose runtime, foundation, material3, ui

### Step 1.2: Create Data Models
**Location:** `shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/models/`

**File: ComponentInstance.kt**
```kotlin
@Serializable
data class ComponentInstance(
    val id: String = Clock.System.now().toString(),
    val name: String,                               // e.g., "Button", "Card", "Text"
    val properties: Map<String, String> = emptyMap(), // e.g., {"label": "Submit", "color": "Primary"}
    var xOffset: Float = 0f,
    var yOffset: Float = 0f
)
```

**File: LayoutManifest.kt**
```kotlin
@Serializable
data class LayoutManifest(
    val layoutName: String = "default",
    val components: List<ComponentInstance> = emptyList()
)
```

**File: DesignSystemTokens.kt**
```kotlin
@Serializable
data class DesignSystemTokens(
    val availableComponents: List<String>,
    val themeTokens: ThemeTokens
)

@Serializable
data class ThemeTokens(
    val colors: List<String>,
    val spacing: List<Int>
)
```

### Step 1.3: Create Design System Schema
**Location:** `shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/`

**File: DesignSystemSchema.kt**
```kotlin
// Hardcoded schema for POC - can be JSON file later
object DesignSystemSchema {
    val tokens = DesignSystemTokens(
        availableComponents = listOf("Button", "Card", "TextField", "Text", "Checkbox"),
        themeTokens = ThemeTokens(
            colors = listOf("Primary", "Secondary", "Background", "Surface"),
            spacing = listOf(8, 16, 24, 32, 48)
        )
    )
}
```

### Step 1.4: Simple Storage Implementation
**Location:** `shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/storage/`

**File: LayoutStorage.kt**
```kotlin
// Use expect/actual for file I/O
expect class LayoutStorage() {
    suspend fun save(manifest: LayoutManifest): Result<Unit>
    suspend fun load(): Result<LayoutManifest>
}
```

Platform implementations:
- `androidMain/.../LayoutStorage.android.kt` - use Context.filesDir
- `iosMain/.../LayoutStorage.ios.kt` - use NSFileManager
- `jvmMain/.../LayoutStorage.jvm.kt` - use File API
- `jsMain/.../LayoutStorage.js.kt` - use localStorage

## Phase 2: Component Library (Material 3 Wrappers)

### Step 2.1: Create Reusable Components
**Location:** `shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/components/`

Create simple wrappers around Material 3 components that accept property maps:

**File: OrchestratorButton.kt**
```kotlin
@Composable
fun OrchestratorButton(properties: Map<String, String>) {
    Button(onClick = {}) {
        Text(properties["label"] ?: "Button")
    }
}
```

**File: OrchestratorCard.kt**
```kotlin
@Composable
fun OrchestratorCard(properties: Map<String, String>) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(properties["title"] ?: "Card Title")
            Text(properties["subtitle"] ?: "Subtitle")
        }
    }
}
```

**File: OrchestratorTextField.kt**
```kotlin
@Composable
fun OrchestratorTextField(properties: Map<String, String>) {
    var text by remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(properties["label"] ?: "Input") }
    )
}
```

**File: OrchestratorText.kt**
```kotlin
@Composable
fun OrchestratorText(properties: Map<String, String>) {
    val style = when (properties["style"]) {
        "headline" -> MaterialTheme.typography.headlineMedium
        "body" -> MaterialTheme.typography.bodyLarge
        else -> MaterialTheme.typography.bodyMedium
    }
    Text(
        text = properties["text"] ?: "Text",
        style = style
    )
}
```

### Step 2.2: Component Registry
**Location:** `shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/registry/`

**File: ComponentRegistry.kt**
```kotlin
object ComponentRegistry {
    @Composable
    fun render(component: ComponentInstance) {
        when (component.name) {
            "Button" -> OrchestratorButton(component.properties)
            "Card" -> OrchestratorCard(component.properties)
            "TextField" -> OrchestratorTextField(component.properties)
            "Text" -> OrchestratorText(component.properties)
            "Checkbox" -> OrchestratorCheckbox(component.properties)
            else -> Text("Unknown: ${component.name}")
        }
    }
    
    fun getAvailableComponents(): List<String> = 
        listOf("Button", "Card", "TextField", "Text", "Checkbox")
}
```

## Phase 3: The Canvas (Draggable Layout)

### Step 3.1: Orchestrator Canvas
**Location:** `shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/canvas/`

**File: OrchestratorCanvas.kt**
```kotlin
@Composable
fun OrchestratorCanvas(
    manifest: LayoutManifest,
    onComponentMoved: (String, Float, Float) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        manifest.components.forEach { component ->
            Box(
                modifier = Modifier
                    .offset { IntOffset(component.xOffset.roundToInt(), component.yOffset.roundToInt()) }
                    .pointerInput(component.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onComponentMoved(
                                component.id,
                                component.xOffset + dragAmount.x,
                                component.yOffset + dragAmount.y
                            )
                        }
                    }
            ) {
                ComponentRegistry.render(component)
            }
        }
    }
}
```

### Step 3.2: State Manager
**File: LayoutState.kt**
```kotlin
class LayoutState {
    private val _manifest = MutableStateFlow(LayoutManifest())
    val manifest: StateFlow<LayoutManifest> = _manifest.asStateFlow()
    
    fun addComponent(component: ComponentInstance) {
        _manifest.update { it.copy(components = it.components + component) }
    }
    
    fun moveComponent(id: String, x: Float, y: Float) {
        _manifest.update { manifest ->
            manifest.copy(
                components = manifest.components.map { 
                    if (it.id == id) it.copy(xOffset = x, yOffset = y) else it 
                }
            )
        }
    }
    
    fun loadManifest(manifest: LayoutManifest) {
        _manifest.value = manifest
    }
    
    fun clearAll() {
        _manifest.value = LayoutManifest()
    }
}
```

## Phase 4: LLM Integration (Gemini)

### Step 4.1: Gemini Client
**Location:** `shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/llm/`

**File: GeminiClient.kt**
```kotlin
class GeminiClient(private val apiKey: String) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    
    suspend fun generateContent(prompt: String): Result<String> {
        // Call Gemini API endpoint
        // Parse response and extract text
    }
}
```

### Step 4.2: Prompt Builder
**File: PromptBuilder.kt**
```kotlin
object PromptBuilder {
    fun buildSystemPrompt(userInput: String): String {
        val availableComponents = DesignSystemSchema.tokens.availableComponents
        return """
            You are a UI Layout Assistant. Your job is to convert user requests into JSON.
            
            Available components: ${availableComponents.joinToString(", ")}
            
            User request: "$userInput"
            
            Rules:
            - Output ONLY valid JSON array of components
            - Each component must have: name, properties (object), xOffset, yOffset
            - If user doesn't specify position, place at (100, 100) with 80px spacing between items
            - Map user's words to closest component name
            - Extract colors, labels, and text from user's request into properties
            
            Example output:
            [{"name": "Button", "properties": {"label": "Submit", "color": "Primary"}, "xOffset": 100, "yOffset": 100}]
            
            Now convert the user request to JSON:
        """.trimIndent()
    }
}
```

### Step 4.3: Intent Parser
**File: IntentParser.kt**
```kotlin
class IntentParser(private val geminiClient: GeminiClient) {
    suspend fun parseIntent(userInput: String): Result<List<ComponentInstance>> {
        val prompt = PromptBuilder.buildSystemPrompt(userInput)
        
        return geminiClient.generateContent(prompt)
            .mapCatching { response ->
                // Extract JSON from LLM response (may have markdown wrapping)
                val jsonString = extractJson(response)
                Json.decodeFromString<List<ComponentInstance>>(jsonString)
            }
    }
    
    private fun extractJson(response: String): String {
        // Remove markdown code blocks if present
        return response
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
```

## Phase 5: Integration & Testing

### Step 5.1: Orchestrator Facade
**Location:** `shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/`

**File: Orchestrator.kt**
```kotlin
class Orchestrator(
    private val geminiApiKey: String,
    private val storage: LayoutStorage
) {
    private val geminiClient = GeminiClient(geminiApiKey)
    private val intentParser = IntentParser(geminiClient)
    val layoutState = LayoutState()
    
    init {
        // Load saved layout on init
        CoroutineScope(Dispatchers.Default).launch {
            storage.load().onSuccess { manifest ->
                layoutState.loadManifest(manifest)
            }
        }
    }
    
    suspend fun processCommand(userInput: String): Result<Unit> {
        return intentParser.parseIntent(userInput)
            .mapCatching { components ->
                components.forEach { layoutState.addComponent(it) }
                storage.save(layoutState.manifest.value)
            }
    }
    
    suspend fun save() {
        storage.save(layoutState.manifest.value)
    }
}
```

### Step 5.2: Update App.kt
**Location:** `composeApp/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/App.kt`

Replace existing App composable with orchestrator UI:
```kotlin
@Composable
fun App() {
    MaterialTheme {
        val orchestrator = remember { 
            Orchestrator(
                geminiApiKey = "YOUR_API_KEY_HERE", // TODO: Move to config
                storage = LayoutStorage()
            )
        }
        
        val manifest by orchestrator.layoutState.manifest.collectAsState()
        var userInput by remember { mutableStateOf("") }
        var isProcessing by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        
        Column(modifier = Modifier.fillMaxSize()) {
            // Command Input Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text("Type command (e.g., 'Add a blue button')") },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing
                )
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isProcessing = true
                            orchestrator.processCommand(userInput)
                            userInput = ""
                            isProcessing = false
                        }
                    },
                    enabled = userInput.isNotBlank() && !isProcessing
                ) {
                    Text(if (isProcessing) "..." else "Add")
                }
                Button(
                    onClick = { orchestrator.layoutState.clearAll() }
                ) {
                    Text("Clear")
                }
            }
            
            // Canvas Area
            OrchestratorCanvas(
                manifest = manifest,
                onComponentMoved = { id, x, y ->
                    orchestrator.layoutState.moveComponent(id, x, y)
                }
            )
        }
    }
}
```

## Phase 6: Testing Strategy

### Step 6.1: Manual Testing Scenarios
1. **Basic Addition:** Type "add a button" → verify button appears
2. **With Properties:** Type "add a button labeled Submit" → verify label shows "Submit"
3. **Multiple Components:** Type "add a card and a text field" → verify both appear
4. **Drag & Drop:** Drag any component → verify position updates
5. **Persistence:** Add components → close app → reopen → verify components restored
6. **Clear:** Click Clear button → verify canvas empties

### Step 6.2: LLM Response Testing
Create test cases in `shared/src/commonTest/kotlin/.../orchestrator/`:
- Test JSON parsing with valid responses
- Test JSON extraction from markdown-wrapped responses
- Test error handling for malformed JSON
- Test component validation against schema

## Implementation Order

### Week 1: Core Foundation
- [ ] Step 1.2: Create data models (ComponentInstance, LayoutManifest, DesignSystemTokens)
- [ ] Step 1.3: Create DesignSystemSchema object
- [ ] Step 1.4: Implement LayoutStorage expect/actual classes
- [ ] Step 2.1: Build 5 Material component wrappers
- [ ] Step 2.2: Create ComponentRegistry

### Week 2: Canvas & State
- [ ] Step 3.1: Build OrchestratorCanvas with drag support
- [ ] Step 3.2: Implement LayoutState with StateFlow
- [ ] Test manual component addition (hardcode some ComponentInstances)

### Week 3: LLM Integration
- [ ] Step 4.1: Implement GeminiClient
- [ ] Step 4.2: Create PromptBuilder
- [ ] Step 4.3: Build IntentParser
- [ ] Step 5.1: Create Orchestrator facade

### Week 4: UI & Testing
- [ ] Step 5.2: Update App.kt with command input UI
- [ ] Step 6.1: Run manual testing scenarios
- [ ] Step 6.2: Write unit tests
- [ ] Polish error handling and loading states

## File Structure Summary

```
shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/
├── models/
│   ├── ComponentInstance.kt
│   ├── LayoutManifest.kt
│   └── DesignSystemTokens.kt
├── components/
│   ├── OrchestratorButton.kt
│   ├── OrchestratorCard.kt
│   ├── OrchestratorTextField.kt
│   ├── OrchestratorText.kt
│   └── OrchestratorCheckbox.kt
├── registry/
│   └── ComponentRegistry.kt
├── canvas/
│   └── OrchestratorCanvas.kt
├── state/
│   └── LayoutState.kt
├── storage/
│   └── LayoutStorage.kt (expect)
├── llm/
│   ├── GeminiClient.kt
│   ├── PromptBuilder.kt
│   └── IntentParser.kt
├── DesignSystemSchema.kt
└── Orchestrator.kt (main facade)

shared/src/androidMain/kotlin/.../orchestrator/storage/
└── LayoutStorage.android.kt

shared/src/iosMain/kotlin/.../orchestrator/storage/
└── LayoutStorage.ios.kt

shared/src/jvmMain/kotlin/.../orchestrator/storage/
└── LayoutStorage.jvm.kt

shared/src/jsMain/kotlin/.../orchestrator/storage/
└── LayoutStorage.js.kt
```

## Technical Decisions

### Why Material 3?
- Already in dependencies
- Cross-platform support via Compose Multiplatform
- Rich component library (20+ components available)
- Consistent theming system
- No need to build custom UI components

### Why Gemini?
- Free tier available (Google AI Studio)
- Simple REST API
- Good at structured output (JSON)
- No credit card required for testing

### Why Registry Pattern?
- Type-safe at compile time
- No reflection or code generation
- Easy to debug
- Explicit about what components exist
- Can add/remove components by editing one file

### Why expect/actual for Storage?
- Each platform has different file system APIs
- Keeps commonMain clean
- Easy to test with in-memory implementations
- Can optimize per platform (e.g., Android DataStore later)

## Future Enhancements (Out of Scope for POC)

- **Voice Input:** Add platform-specific speech recognition (Android SpeechRecognizer, iOS Speech framework, Web SpeechRecognition API)
- **Layout Templates:** Predefined layouts (Login Screen, Dashboard, Form)
- **Undo/Redo:** Command pattern for history management
- **Component Styling:** More granular control (fontSize, padding, colors as props)
- **Alignment Guides:** Snap-to-grid, spacing helpers
- **Export:** Generate Kotlin code from layout manifest
- **Cloud Sync:** Share layouts across devices
- **Component Nesting:** Parent-child relationships (Card contains Button)
- **Event Handlers:** Wire up onClick, onValueChange to actual logic

## API Key Management

For Gemini API key, create platform-specific configuration:

**Option 1: Environment Variable (Recommended for POC)**
```kotlin
// In Platform.kt, add:
expect fun getGeminiApiKey(): String

// Android: Read from BuildConfig or local.properties
// iOS: Read from Info.plist
// JVM: Read from environment variable
// JS: Read from localStorage or config
```

**Option 2: User Input (Simplest for POC)**
- Add a settings screen where user pastes their Gemini API key
- Store in LayoutStorage
- Load on app start

## Success Criteria

The POC is complete when:
1. User can type "add a blue button" and see a Material button appear
2. User can drag components around the canvas
3. Layout persists when app is closed and reopened
4. System works on at least 2 platforms (Android + Desktop recommended)
5. Handles basic errors (invalid LLM response, network failure)

## Next Steps

1. Review this plan and confirm approach
2. Set up Gemini API key (https://makersuite.google.com/app/apikey)
3. Start with Phase 1: Create data models
4. Build incrementally, testing each phase before moving forward

