# Phase 1 Execution Analysis: Foundation (Data & Storage)

## Current State Assessment

### ✅ What's Already Done
- Serialization plugin added to build.gradle.kts
- All required dependencies added:
  - kotlinx-serialization-json (1.7.3)
  - kotlinx-coroutines-core (1.10.2)
  - Ktor client with platform-specific engines
  - kotlinx-datetime (0.6.1)
  - Compose dependencies (runtime, foundation, material3, ui)
- KMP targets configured: Android, iOS, JVM, JS, WasmJS

### 📋 What Needs to Be Built

## Step 1.2: Create Data Models

### Files to Create:
```
shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/
└── models/
    ├── ComponentInstance.kt
    ├── LayoutManifest.kt
    └── DesignSystemTokens.kt
```

### Analysis: ComponentInstance.kt

**Purpose:** Represents a single UI component instance on the canvas

**Key Decisions:**
1. **ID Generation:** Using `Clock.System.now().toString()` is quick but has collision risk if two components added in same millisecond
   - **Better approach:** Use UUID or combine timestamp + random
   - **Trade-off:** Adds dependency vs. simple timestamp
   - **Recommendation:** Use `"${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt()}"` (no extra dependency)

2. **Properties as Map<String, String>:**
   - **Pro:** Flexible, works with any component type
   - **Con:** No type safety, everything is a string
   - **Alternative:** Use `Map<String, JsonElement>` for nested objects
   - **Recommendation:** Start with `Map<String, String>`, expand later if needed

3. **Position as Float:**
   - **Pro:** Precise positioning
   - **Con:** Need to convert to Int for Compose offset
   - **Alternative:** Use Int directly
   - **Recommendation:** Keep Float for smooth dragging, convert to Int in UI layer

**Complexity:** 🟢 Low - Simple data class, no business logic

### Analysis: LayoutManifest.kt

**Purpose:** Container for all components in a layout, the "save file" format

**Key Decisions:**
1. **Default Empty List:**
   - Good for initial state
   - Makes creation easy: `LayoutManifest()`

2. **Layout Name:**
   - Currently just "default"
   - **Future:** Could support multiple named layouts (Home, Profile, Settings)
   - **Recommendation:** Keep simple for POC, single layout only

**Complexity:** 🟢 Low - Just a wrapper class

### Analysis: DesignSystemTokens.kt

**Purpose:** Schema definition for available components and theme constraints

**Key Decisions:**
1. **Schema Location:**
   - Plan shows both JSON file option and hardcoded Kotlin object
   - **JSON File Pros:** Easy to edit, no recompile, can be downloaded remotely
   - **JSON File Cons:** Need to bundle resource, parse on startup, handle missing file
   - **Kotlin Object Pros:** Type-safe, compile-time validation, no I/O
   - **Kotlin Object Cons:** Need recompile to change
   - **Recommendation:** Start with Kotlin object, migrate to JSON later

2. **Token Structure:**
   - Colors: List<String> vs enum class
   - Spacing: List<Int> vs sealed class hierarchy
   - **Recommendation:** Simple lists for POC, can add validation later

**Complexity:** 🟢 Low - Just data structures

## Step 1.3: Create Design System Schema

### File to Create:
```
shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/
└── DesignSystemSchema.kt
```

### Analysis: DesignSystemSchema.kt

**Purpose:** Hardcoded schema object that defines what components are available

**Key Decisions:**
1. **Material 3 Component Selection:**
   - Plan suggests: Button, Card, TextField, Text, Checkbox
   - **Consider adding:**
     - Switch (common in UI)
     - Slider (for settings)
     - CircularProgressIndicator (for loading states)
   - **Recommendation:** Start with 5, easy to add more

2. **Color Naming:**
   - Material 3 uses: primary, secondary, tertiary, error, background, surface, etc.
   - Should we map user's "blue" to "primary"?
   - **Recommendation:** Include both friendly names ("blue", "red") and Material names ("Primary", "Secondary")

3. **Spacing Values:**
   - Material uses 4dp grid: 4, 8, 12, 16, 24, 32, 48, 64
   - Plan shows: 8, 16, 24, 32, 48
   - **Recommendation:** Use Material's standard scale

**Complexity:** 🟢 Low - Static object, no logic

**Question to Resolve:** Do we need spacing/color tokens in Phase 1, or can we defer theme customization to later phases?

## Step 1.4: Simple Storage Implementation

### Files to Create:
```
shared/src/commonMain/kotlin/.../orchestrator/storage/
└── LayoutStorage.kt (expect)

shared/src/androidMain/kotlin/.../orchestrator/storage/
└── LayoutStorage.android.kt (actual)

shared/src/iosMain/kotlin/.../orchestrator/storage/
└── LayoutStorage.ios.kt (actual)

shared/src/jvmMain/kotlin/.../orchestrator/storage/
└── LayoutStorage.jvm.kt (actual)

shared/src/jsMain/kotlin/.../orchestrator/storage/
└── LayoutStorage.js.kt (actual)

shared/src/wasmJsMain/kotlin/.../orchestrator/storage/
└── LayoutStorage.wasmJs.kt (actual)
```

### Analysis: LayoutStorage Implementation

**Complexity:** 🟡 Medium - Requires platform-specific code

**Key Decisions:**

1. **Storage Strategy Options:**
   
   **Option A: File-based (Plan's approach)**
   - Write JSON to file system
   - Pros: Simple, human-readable, can inspect files
   - Cons: Need platform-specific paths, permissions on mobile
   
   **Option B: In-Memory Only (Simplest for POC)**
   - Just keep in StateFlow, no persistence
   - Pros: Zero platform code needed immediately
   - Cons: Data lost on app restart
   
   **Option C: Multiplatform-Settings Library**
   - Use existing library: `com.russhwolf:multiplatform-settings`
   - Pros: Handles all platforms, simple API
   - Cons: Adds dependency, limited to key-value (need to serialize whole JSON)
   
   **Recommendation:** Start with Option B for immediate testing, implement Option A in parallel

2. **Platform-Specific Challenges:**

   **Android:**
   - Need Context to access filesDir
   - How to pass Context to expect/actual class?
   - **Solution:** Make LayoutStorage a class with constructor: `actual class LayoutStorage(private val context: Context)`
   - **Issue:** Context not available in commonMain
   - **Better Solution:** Use `expect fun getStoragePath(): String` instead

   **iOS:**
   - Use NSFileManager and NSSearchPathForDirectoriesInDomains
   - Swift/ObjC interop required
   - **Challenge:** Kotlin/Native file APIs

   **JVM (Desktop):**
   - Use `System.getProperty("user.home")` + app folder
   - Standard File API works
   - **Easiest platform**

   **JS/WasmJS:**
   - Use localStorage (limited to ~5-10MB)
   - Need to encode JSON as string
   - **Challenge:** Different API than file system

3. **File Format:**
   ```json
   {
     "layoutName": "default",
     "components": [
       {
         "id": "1234567890-12345",
         "name": "Button",
         "properties": {"label": "Submit", "color": "Primary"},
         "xOffset": 100.0,
         "yOffset": 200.0
       }
     ]
   }
   ```
   - **File name:** `layout_manifest.json` or `orchestrator_layout.json`?
   - **Recommendation:** `orchestrator_layout.json` (more specific)

4. **Error Handling:**
   - File doesn't exist (first launch) → Return empty LayoutManifest
   - Corrupted JSON → Return empty LayoutManifest + log error
   - Permission denied → Return Result.failure with error message
   - **Use:** `Result<LayoutManifest>` for all operations

5. **Concurrency:**
   - Multiple save calls while dragging → Need debouncing?
   - Read while writing → Need file locking?
   - **Recommendation:** Use `Mutex` to serialize save operations
   - **Alternative:** Debounce saves (only save 500ms after last move)

## Execution Blockers & Dependencies

### Critical Questions:

1. **Android Context Problem:**
   - How do we get Context in shared module?
   - **Options:**
     - Pass from App during Orchestrator init
     - Use expect/actual for storage path resolver
     - Use AndroidX startup library
   - **Recommendation:** Expect/actual path resolver is cleanest

2. **Immediate Testing:**
   - Can't test storage until we have UI to add components
   - **Solution:** Write unit tests with in-memory implementation first

3. **WasmJS Support:**
   - Do we need WasmJS storage for POC?
   - **Recommendation:** Make it no-op initially (just return empty manifest)

## Recommended Execution Order

### Part A: Data Models (30 minutes)
1. Create `models/` directory structure
2. Write `ComponentInstance.kt` - 10 lines
3. Write `LayoutManifest.kt` - 5 lines
4. Write `DesignSystemTokens.kt` - 15 lines
5. Sync Gradle, verify no compilation errors

### Part B: Design System Schema (15 minutes)
1. Create `DesignSystemSchema.kt`
2. Define 5 Material components
3. Add Material color scheme names
4. Add standard spacing scale

### Part C: Storage Interface (15 minutes)
1. Create `storage/LayoutStorage.kt` with expect class
2. Define interface: `suspend fun save()`, `suspend fun load()`
3. Return `Result<T>` for error handling

### Part D: Platform Implementations (2-3 hours)
1. **Start with JVM** (easiest to test on desktop)
   - Implement file write/read in user home directory
   - Test with simple main() function
2. **Then Android**
   - Create expect/actual for storage path
   - Write to app's internal storage
3. **Then iOS**
   - Use document directory
   - Handle Kotlin/Native file APIs
4. **Finally JS/WasmJS**
   - Implement with localStorage
   - Handle serialization differences

### Part E: Validation (30 minutes)
1. Write unit tests for serialization/deserialization
2. Test save → load round trip
3. Test error cases (corrupted JSON, missing file)

## Risk Assessment

### 🔴 High Risk:
- **Platform-specific storage implementations** - Each platform has different APIs
  - Mitigation: Start with JVM only, add platforms incrementally
  - Can also defer to Phase 3 (test with in-memory first)

### 🟡 Medium Risk:
- **Dependency sync issues** - Added many new dependencies
  - Mitigation: Sync Gradle immediately after changes
  - Verify build works before writing code

### 🟢 Low Risk:
- **Data models** - Straightforward Kotlin classes
- **Design system schema** - Just static data

## Time Estimate

- **Minimum (JVM only):** 2-3 hours
- **Full (all platforms):** 6-8 hours
- **With thorough testing:** 10-12 hours

## Success Criteria for Phase 1

✅ Phase 1 is complete when:
1. Can create a `ComponentInstance` and serialize it to JSON
2. Can create a `LayoutManifest` with multiple components
3. Can save manifest to disk/storage on at least ONE platform (JVM recommended)
4. Can load manifest back and get same data
5. Build compiles on all target platforms (even if storage is no-op on some)
6. Unit tests pass for serialization/deserialization

## Recommendation: Simplified Phase 1

To move faster, consider this **minimal viable Phase 1**:

### Simplified Step 1: Data Models Only (1 hour)
- Create the 3 data classes
- Create DesignSystemSchema object
- Write serialization tests
- **Skip storage implementation entirely**

### Simplified Step 2: Mock Storage (30 minutes)
```kotlin
class LayoutStorage {
    private var cached: LayoutManifest? = null
    
    suspend fun save(manifest: LayoutManifest): Result<Unit> {
        cached = manifest
        return Result.success(Unit)
    }
    
    suspend fun load(): Result<LayoutManifest> {
        return Result.success(cached ?: LayoutManifest())
    }
}
```

This lets you move to Phase 2 & 3 (building the canvas), then come back to real persistence later.

## Next Steps

Which approach do you prefer:
1. **Full Phase 1:** Build all platform storage implementations now
2. **Minimal Phase 1:** Just data models + in-memory storage, defer real persistence
3. **Hybrid:** Data models + JVM storage only (test on Desktop first)

I recommend **Option 3 (Hybrid)** - gets you persistent storage for testing but only on one platform, which is much faster to implement.

