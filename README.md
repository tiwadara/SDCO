# 🎙️ Voice-to-Layout POC

A Kotlin Multiplatform proof-of-concept that converts natural language commands into interactive UI layouts using Google's Gemini LLM.

**Input:** "Add a login form with email, password, and submit button"  
**Output:** Material 3 components appear on a draggable canvas, persisted locally.

## ✨ Features

- **Natural language → UI** — Type a command, Gemini generates components
- **7 Material 3 components** — Button, Text, TextField, Card, Checkbox, Switch, Image
- **Drag-and-drop canvas** — Reposition components freely
- **Persistent storage** — Layouts saved/restored automatically
- **Cross-platform** — Android, iOS, Desktop (JVM), Web (JS/WasmJS)
- **Dynamic model selection** — Fetches available Gemini models for your API key

## 🚀 Quick Start

### Prerequisites
- JDK 11+
- Android SDK (for Android target)
- A [Google AI Studio](https://aistudio.google.com/app/apikey) API key

### Run Desktop (fastest to test)
```bash
./gradlew :composeApp:run
```

### Run Web (JS)
```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
```

### Run Android
```bash
./gradlew :composeApp:installDebug
```

### Run Tests
```bash
./gradlew :shared:jvmTest
```

## 🏗️ Architecture

```
User Input → PromptBuilder → GeminiClient → IntentParser → LayoutState → Canvas
                                                              ↕
                                                         LayoutStorage
```

### Module Structure
```
shared/src/commonMain/.../orchestrator/
├── models/          # ComponentInstance, LayoutManifest, DesignSystemTokens
├── components/      # 7 Material 3 wrappers (OrchestratorButton, etc.)
├── registry/        # ComponentRegistry — name → Composable lookup
├── canvas/          # OrchestratorCanvas — draggable layout
├── state/           # LayoutState — StateFlow-based state management
├── storage/         # LayoutStorage — expect/actual per platform
├── llm/             # GeminiClient, PromptBuilder, IntentParser
├── DesignSystemSchema.kt
└── Orchestrator.kt  # Top-level facade
```

## 📦 Deploy Web Version

The web version can be deployed to GitHub Pages:

```bash
# Build production JS bundle
./gradlew :composeApp:jsBrowserDistribution

# Output is in: composeApp/build/dist/js/productionExecutable/
```

See the GitHub Actions workflow in `.github/workflows/deploy-web.yml` for automated deployment.

## 🧪 Test Coverage

58 unit tests covering:
- Data model serialization round-trips
- Design system schema validation
- Component registry ↔ schema sync
- LayoutState mutations, bounds clamping, edge cases
- LLM response JSON extraction (markdown fences, leading/trailing text)
- Prompt builder content verification

## 📄 License

MIT
