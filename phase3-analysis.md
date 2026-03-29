# Phase 3 Execution Analysis: The Canvas (Draggable Layout)

## Overview

Phase 3 is where the magic happens - building the interactive canvas that displays components, allows drag-and-drop, and manages layout state. This is the visual heart of the orchestrator.

## Current State

### ✅ Prerequisites from Previous Phases
- **Phase 1:** ComponentInstance, LayoutManifest data models
- **Phase 2:** ComponentRegistry.render() working, 5 components available
- **Dependencies:** Compose UI, Foundation, gestures already added

### 📋 What Phase 3 Delivers
- Interactive canvas that renders component tree
- Drag-and-drop functionality for repositioning components
- StateFlow-based state management for reactive updates
- Foundation for persistence (save/load will use this state)

---

## Step 3.1: Orchestrator Canvas

### File to Create:
```
shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/
└── canvas/
    └── OrchestratorCanvas.kt
```

### Analysis: OrchestratorCanvas.kt

**Purpose:** The main composable that renders all components at their positions with drag support

**Planned Architecture:**
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

---

## Key Technical Challenges

### Challenge 1: Offset vs Absolute Positioning

**Problem:** Compose has multiple positioning strategies

**Options:**

1. **Using Modifier.offset {} (Planned approach)**
   ```kotlin
   .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
   ```
   - ✅ Relative to parent Box
   - ✅ Works with any parent layout
   - ❌ Requires lambda allocation per component
   - ❌ Float → Int conversion every recomposition

2. **Using Layout Modifier**
   ```kotlin
   .layout { measurable, constraints ->
       val placeable = measurable.measure(constraints)
       layout(placeable.width, placeable.height) {
           placeable.place(x.roundToInt(), y.roundToInt())
       }
   }
   ```
   - ✅ More control over layout
   - ✅ Can adjust size dynamically
   - ❌ More complex code
   - ❌ Overkill for simple positioning

3. **Using absoluteOffset**
   ```kotlin
   .absoluteOffset(x.roundToInt().dp, y.roundToInt().dp)
   ```
   - ✅ Simpler API
   - ❌ Doesn't work with pixels, needs dp conversion
   - ❌ Doesn't handle density properly for drag

**Recommendation:** Use Modifier.offset {} as planned - it's the standard approach for draggable items

**Optimization:** Use offset(x.dp, y.dp) non-lambda version if performance becomes issue

---

### Challenge 2: Drag Gesture Implementation

**Problem:** Need smooth dragging that updates state reactively

**Current Plan:**
```kotlin
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
```

**Analysis:**

1. **pointerInput Key:**
   - Uses `component.id` as key
   - **Critical:** When ID changes, gesture detector resets
   - **Issue:** If component recreated (same visual position, new ID), drag state lost
   - **Recommendation:** Keep as-is, acceptable for POC

2. **change.consume():**
   - Prevents event from propagating to parent
   - **Important:** Without this, scrolling parent might interfere
   - **Good practice:** Always consume when handling drag

3. **Coordinate Calculation:**
   ```kotlin
   component.xOffset + dragAmount.x
   ```
   - **Issue:** dragAmount is in pixels, xOffset is Float
   - **Question:** Are they the same unit system?
   - **Answer:** Yes, both use pixels in Compose coordinate system
   - **Edge case:** What about negative coordinates? (dragging off-screen left/top)
   - **Recommendation:** Add bounds checking in state manager

4. **Alternative Gesture APIs:**

   **Option A: detectDragGestures (Current)**
   - Simple, handles entire drag lifecycle
   - Updates continuously during drag
   
   **Option B: detectDragGesturesAfterLongPress**
   ```kotlin
   detectDragGesturesAfterLongPress { change, dragAmount -> ... }
   ```
   - Prevents accidental drags
   - Better UX for components with click actions
   - **Recommendation:** Consider for future, adds friction for POC
   
   **Option C: Modifier.draggable**
   ```kotlin
   .draggable(
       state = rememberDraggableState { delta -> ... },
       orientation = Orientation.Vertical
   )
   ```
   - Only supports single-axis dragging
   - Not suitable for 2D canvas
   - **Don't use**

**Complexity Assessment:** 🟡 Medium - Gesture handling is tricky but well-documented

---

### Challenge 3: Component Z-Index / Layering

**Problem:** Components can overlap, which one shows on top?

**Current Behavior:**
```kotlin
manifest.components.forEach { component -> ... }
```
- Components rendered in list order
- Later components appear on top
- **Issue:** No way to reorder z-index dynamically

**Scenarios:**
1. User adds Button at (100, 100)
2. User adds Card at (110, 110) - overlaps button
3. Card appears on top (added later)
4. User wants to drag Button on top - **not possible**

**Solutions:**

1. **Accept Fixed Order (POC approach):**
   - Last added = top layer
   - Document as limitation
   - **Simple, no code changes**

2. **Add Z-Index Property:**
   ```kotlin
   @Serializable
   data class ComponentInstance(
       // ...existing properties...
       var zIndex: Int = 0
   )
   
   // In canvas:
   manifest.components.sortedBy { it.zIndex }.forEach { ... }
   ```
   - **Pro:** Flexible ordering
   - **Con:** Need UI to adjust z-index
   - **Recommendation:** Add property but don't expose UI yet

3. **Bring-to-Front on Drag:**
   - When user starts dragging, move to end of list
   - **Pro:** Intuitive UX
   - **Con:** Modifies component order, affects save/load
   - **Recommendation:** Good enhancement for later

**Recommendation:** Start with fixed order, add zIndex property but don't use it yet

---

### Challenge 4: Canvas Bounds & Scrolling

**Problem:** Canvas is fullScreen, but components can be dragged anywhere

**Questions:**
1. Should canvas be scrollable?
2. What happens if component dragged to negative coordinates?
3. What's the maximum canvas size?

**Options:**

1. **Infinite Canvas (Planned)**
   ```kotlin
   Box(modifier = Modifier.fillMaxSize()) { ... }
   ```
   - ✅ Simple, no bounds checking
   - ❌ Components can be dragged off-screen
   - ❌ No way to scroll to see off-screen components
   - **Risk:** User loses components

2. **Bounded Canvas**
   ```kotlin
   fun onComponentMoved(id: String, x: Float, y: Float) {
       val boundedX = x.coerceIn(0f, maxCanvasWidth)
       val boundedY = y.coerceIn(0f, maxCanvasHeight)
       layoutState.moveComponent(id, boundedX, boundedY)
   }
   ```
   - ✅ Components stay on screen
   - ❌ Need to define max width/height
   - **Recommendation:** Good for POC

3. **Scrollable Canvas**
   ```kotlin
   Box(
       modifier = Modifier
           .fillMaxSize()
           .verticalScroll(rememberScrollState())
           .horizontalScroll(rememberScrollState())
   ) { ... }
   ```
   - ✅ Can have large canvas, scroll to view
   - ❌ Drag gestures conflict with scroll gestures
   - ❌ Need to handle nested scrolling
   - **Complex:** Not recommended for POC

**Recommendation:** Use Option 2 (bounded canvas) with reasonable limits:
- Width: 2000f (about 3-4 screen widths)
- Height: 3000f (about 5-6 screen heights)
- Add bounds checking in LayoutState.moveComponent()

---

### Challenge 5: Component Selection Visual Feedback

**Problem:** User can't tell which component is selected/being dragged

**Current Plan:** No selection feedback

**Enhancement Ideas:**

1. **Border on Drag:**
   ```kotlin
   var isDragging by remember { mutableStateOf(false) }
   
   Box(
       modifier = Modifier
           .border(
               width = if (isDragging) 2.dp else 0.dp,
               color = MaterialTheme.colorScheme.primary
           )
           .pointerInput(component.id) {
               detectDragGestures(
                   onDragStart = { isDragging = true },
                   onDragEnd = { isDragging = false },
                   onDrag = { change, dragAmount -> ... }
               )
           }
   ) { ... }
   ```
   - **Pro:** Clear visual feedback
   - **Con:** Need to track drag state per component
   - **Recommendation:** Add if time permits

2. **Shadow/Elevation on Drag:**
   ```kotlin
   .shadow(elevation = if (isDragging) 8.dp else 0.dp)
   ```
   - **Pro:** Looks polished
   - **Con:** Same state tracking needed

3. **Selection State:**
   - Add "selectedComponentId" to LayoutState
   - Click to select, shows border
   - Delete button to remove selected component
   - **Recommendation:** Good feature, defer to Phase 5

**Recommendation:** Start without selection feedback, add drag border if Phase 3 finishes early

---

## Step 3.2: State Manager

### File to Create:
```
shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/
└── state/
    └── LayoutState.kt
```

### Analysis: LayoutState.kt

**Purpose:** Central state manager using StateFlow for reactive UI updates

**Planned Architecture:**
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

---

## Key Design Decisions

### Decision 1: MutableStateFlow vs MutableState

**Option A: StateFlow (Planned)**
```kotlin
private val _manifest = MutableStateFlow(LayoutManifest())
val manifest: StateFlow<LayoutManifest> = _manifest.asStateFlow()

// In Composable:
val manifest by layoutState.manifest.collectAsState()
```
- ✅ Works across all KMP targets
- ✅ Survives recomposition
- ✅ Can be observed from ViewModels, coroutines
- ✅ Lifecycle aware
- ❌ Requires .collectAsState() in Composables

**Option B: MutableState**
```kotlin
var manifest by mutableStateOf(LayoutManifest())
    private set

// In Composable:
val currentManifest = layoutState.manifest
```
- ✅ Simpler Compose integration
- ✅ No collectAsState() needed
- ❌ Only works in Compose scope
- ❌ Less flexible for non-UI observers

**Recommendation:** Keep StateFlow - more flexible, better for later phases (persistence, undo/redo)

---

### Decision 2: Immutable State Updates

**Current Approach:**
```kotlin
_manifest.update { it.copy(components = it.components + component) }
```
- Creates new LayoutManifest with new component list
- Immutable data structures

**Why Immutability?**
1. **StateFlow requires new object** to trigger observers
2. **Easier debugging** - can track state history
3. **Thread safety** - no concurrent modification issues
4. **Undo/redo support** - can keep history of manifests

**Performance Consideration:**
- Copying list on every move during drag = many allocations
- For POC with <50 components: **not a problem**
- For production with 100s of components: **might need optimization**

**Optimization Strategies (future):**
```kotlin
// Option 1: Debounce drag updates
var dragJob: Job? = null
fun moveComponent(id: String, x: Float, y: Float) {
    dragJob?.cancel()
    dragJob = scope.launch {
        delay(16) // ~60fps
        _manifest.update { ... }
    }
}

// Option 2: Mutable approach with manual notification
private val components = mutableListOf<ComponentInstance>()
fun moveComponent(id: String, x: Float, y: Float) {
    components.find { it.id == id }?.apply {
        xOffset = x
        yOffset = y
    }
    _manifest.value = _manifest.value.copy() // Trigger update
}
```

**Recommendation:** Start with immutable, optimize only if performance issue observed

---

### Decision 3: State Management Operations

**What operations do we need?**

**Current Plan (Minimum):**
- ✅ addComponent() - Add new component
- ✅ moveComponent() - Update position
- ✅ loadManifest() - Load from storage
- ✅ clearAll() - Reset canvas

**Missing Operations (Consider Adding):**
1. **removeComponent(id: String)** - Delete component
   ```kotlin
   fun removeComponent(id: String) {
       _manifest.update { 
           it.copy(components = it.components.filter { c -> c.id != id })
       }
   }
   ```
   - **Essential** for fixing mistakes
   - **Recommendation:** Add immediately

2. **updateComponentProperties(id: String, properties: Map<String, String>)**
   ```kotlin
   fun updateComponentProperties(id: String, newProps: Map<String, String>) {
       _manifest.update { manifest ->
           manifest.copy(
               components = manifest.components.map {
                   if (it.id == id) it.copy(properties = it.properties + newProps)
                   else it
               }
           )
       }
   }
   ```
   - **Useful** for editing labels, colors after creation
   - **Recommendation:** Add in Phase 5 (UI for editing)

3. **getComponentById(id: String): ComponentInstance?**
   ```kotlin
   fun getComponentById(id: String): ComponentInstance? {
       return _manifest.value.components.find { it.id == id }
   }
   ```
   - **Helper** for selection/editing features
   - **Recommendation:** Add when needed

4. **duplicateComponent(id: String)**
   ```kotlin
   fun duplicateComponent(id: String) {
       val original = getComponentById(id) ?: return
       val duplicate = original.copy(
           id = "${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt()}",
           xOffset = original.xOffset + 20f,
           yOffset = original.yOffset + 20f
       )
       addComponent(duplicate)
   }
   ```
   - **Nice to have** for productivity
   - **Recommendation:** Future enhancement

**Recommendation:** Add removeComponent() now, others later

---

### Decision 4: Error Handling

**What can go wrong?**

1. **Moving non-existent component:**
   ```kotlin
   fun moveComponent(id: String, x: Float, y: Float) {
       _manifest.update { manifest ->
           val found = manifest.components.any { it.id == id }
           if (!found) {
               // What to do?
               println("Warning: Component $id not found")
           }
           manifest.copy(
               components = manifest.components.map {
                   if (it.id == id) it.copy(xOffset = x, yOffset = y) else it
               }
           )
       }
   }
   ```
   - **Option A:** Silent failure (current)
   - **Option B:** Log warning
   - **Option C:** Throw exception
   - **Recommendation:** Log warning for debugging

2. **Adding duplicate ID:**
   - Currently possible to add component with existing ID
   - **Issue:** moveComponent() would update multiple components
   - **Recommendation:** Check for duplicates in addComponent()

3. **Invalid coordinates:**
   - Negative values, NaN, Infinity
   - **Recommendation:** Add validation in moveComponent()

**Enhanced Implementation:**
```kotlin
fun moveComponent(id: String, x: Float, y: Float) {
    // Validation
    if (!x.isFinite() || !y.isFinite()) {
        println("Error: Invalid coordinates for $id: ($x, $y)")
        return
    }
    
    // Bounds checking
    val boundedX = x.coerceIn(0f, MAX_CANVAS_WIDTH)
    val boundedY = y.coerceIn(0f, MAX_CANVAS_HEIGHT)
    
    _manifest.update { manifest ->
        if (manifest.components.none { it.id == id }) {
            println("Warning: Component $id not found")
        }
        manifest.copy(
            components = manifest.components.map {
                if (it.id == id) it.copy(xOffset = boundedX, yOffset = boundedY)
                else it
            }
        )
    }
}
```

---

## Implementation Plan

### Part A: Basic Canvas (1 hour)

**Step 1: Create directory structure**
```
shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/
├── canvas/
│   └── OrchestratorCanvas.kt
└── state/
    └── LayoutState.kt
```

**Step 2: Implement LayoutState.kt (30 min)**
- Create class with StateFlow
- Implement addComponent(), moveComponent(), clearAll(), loadManifest()
- Add removeComponent() for completeness
- Add basic validation

**Step 3: Implement OrchestratorCanvas.kt (30 min)**
- Create composable with Box layout
- Add forEach loop for components
- Add offset modifier
- Add ComponentRegistry.render() call

---

### Part B: Add Drag Gestures (1 hour)

**Step 4: Import gesture dependencies**
```kotlin
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
```

**Step 5: Add pointerInput modifier (30 min)**
- Implement detectDragGestures
- Wire up onComponentMoved callback
- Test drag behavior

**Step 6: Add bounds checking (30 min)**
- Define MAX_CANVAS_WIDTH, MAX_CANVAS_HEIGHT constants
- Add coerceIn() to moveComponent()
- Test dragging to edges

---

### Part C: Testing & Polish (30 min - 1 hour)

**Step 7: Create test harness in composeApp**
```kotlin
@Composable
fun CanvasTestScreen() {
    val layoutState = remember { LayoutState() }
    val manifest by layoutState.manifest.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Test buttons
        Row(modifier = Modifier.padding(16.dp)) {
            Button(onClick = {
                layoutState.addComponent(
                    ComponentInstance(
                        name = "Button",
                        properties = mapOf("label" to "Test ${Random.nextInt(100)}"),
                        xOffset = Random.nextFloat() * 300,
                        yOffset = Random.nextFloat() * 300
                    )
                )
            }) {
                Text("Add Button")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { layoutState.clearAll() }) {
                Text("Clear")
            }
        }
        
        // Canvas
        OrchestratorCanvas(
            manifest = manifest,
            onComponentMoved = { id, x, y ->
                layoutState.moveComponent(id, x, y)
            }
        )
    }
}
```

**Step 8: Manual testing**
- Add multiple components
- Drag each component
- Verify smooth updates
- Test edge cases (drag to bounds, overlapping components)

---

## Testing Strategy

### Unit Tests (Optional but Recommended)

**Test LayoutState in commonTest:**
```kotlin
class LayoutStateTest {
    @Test
    fun testAddComponent() {
        val state = LayoutState()
        val component = ComponentInstance(name = "Button", properties = emptyMap())
        
        state.addComponent(component)
        
        assertEquals(1, state.manifest.value.components.size)
        assertEquals("Button", state.manifest.value.components.first().name)
    }
    
    @Test
    fun testMoveComponent() {
        val state = LayoutState()
        val component = ComponentInstance(id = "test-1", name = "Button", properties = emptyMap())
        state.addComponent(component)
        
        state.moveComponent("test-1", 100f, 200f)
        
        val moved = state.manifest.value.components.first()
        assertEquals(100f, moved.xOffset)
        assertEquals(200f, moved.yOffset)
    }
    
    @Test
    fun testMoveComponentWithBounds() {
        val state = LayoutState()
        val component = ComponentInstance(id = "test-1", name = "Button", properties = emptyMap())
        state.addComponent(component)
        
        state.moveComponent("test-1", -100f, -200f) // Negative coords
        
        val moved = state.manifest.value.components.first()
        assertEquals(0f, moved.xOffset) // Should be clamped to 0
        assertEquals(0f, moved.yOffset)
    }
    
    @Test
    fun testRemoveComponent() {
        val state = LayoutState()
        val component = ComponentInstance(id = "test-1", name = "Button", properties = emptyMap())
        state.addComponent(component)
        
        state.removeComponent("test-1")
        
        assertEquals(0, state.manifest.value.components.size)
    }
}
```

### Manual Test Cases

**Basic Functionality:**
1. ✅ Add component via button → appears on canvas
2. ✅ Add multiple components → all visible
3. ✅ Drag component → position updates smoothly
4. ✅ Drag to edge → stops at bounds
5. ✅ Clear button → removes all components

**Edge Cases:**
6. ✅ Add 20+ components → performance still good
7. ✅ Drag component off-screen → stays within bounds
8. ✅ Overlapping components → can drag both
9. ✅ Rapid dragging → no lag or crashes
10. ✅ Component added at specific position → renders at correct location

**Interaction Tests:**
11. ✅ Click button component while dragging → no click fires (drag takes precedence)
12. ✅ Type in TextField → state works (even if not persisted)
13. ✅ Drag TextField → doesn't interfere with text input

---

## Risk Assessment

### 🟢 Low Risk Items:
- **LayoutState implementation** - Standard StateFlow pattern
- **Basic offset positioning** - Well-documented Compose API
- **ComponentRegistry integration** - Already built in Phase 2

### 🟡 Medium Risk Items:

1. **Drag Performance:**
   - **Risk:** Lag with many components during drag
   - **Mitigation:** Start with immutable updates, optimize if needed
   - **Impact:** Medium (UX issue, not functional)
   - **Test with:** 50+ components

2. **Gesture Conflicts:**
   - **Risk:** Drag interferes with component interactions (TextField, Checkbox)
   - **Mitigation:** change.consume() should prevent bubbling
   - **Impact:** Medium (some components might not work)
   - **Test:** Type in TextField, check checkbox while canvas has drag enabled

3. **Coordinate System Confusion:**
   - **Risk:** Mixing dp and pixels, density scaling issues
   - **Mitigation:** Use Float pixels consistently, test on different devices
   - **Impact:** Medium (components might be positioned incorrectly)

### 🔴 High Risk Items:

1. **Multi-Platform Gesture Differences:**
   - **Risk:** Touch (mobile) vs mouse (desktop) vs trackpad gestures behave differently
   - **Mitigation:** Test on at least 2 platforms (Android + JVM desktop)
   - **Impact:** High (might not work on some platforms)
   - **Critical:** Must validate gesture detection works cross-platform

2. **StateFlow Collection in Compose:**
   - **Risk:** Improper collection leads to memory leaks or missed updates
   - **Mitigation:** Use collectAsState() correctly, not collectAsStateWithLifecycle() in shared module
   - **Impact:** High (broken reactivity = broken app)
   - **Validation:** Test adding component updates UI immediately

---

## Platform-Specific Considerations

### Desktop (JVM):
- ✅ Mouse drag events work with detectDragGestures
- ⚠️ Right-click might interfere (consider context menus later)
- Test: Drag with mouse, scroll with trackpad

### Android:
- ✅ Touch events native to Compose
- ⚠️ Edge swipe gestures might conflict with system navigation
- Test: Drag with finger, multi-touch

### iOS:
- ✅ Should work same as Android (touch events)
- ⚠️ Edge swipes for back navigation might conflict
- Test: Native gestures don't break canvas

### Web (JS):
- ⚠️ Mouse events might need different handling
- ⚠️ Pointer events API differences
- **Recommendation:** Test early on web, might need platform-specific code

---

## Performance Optimization Strategies

### If Performance Issues Occur:

**Problem: Lag during drag**

**Solution 1: Debounce State Updates**
```kotlin
private var dragUpdateJob: Job? = null

fun moveComponentDebounced(id: String, x: Float, y: Float) {
    dragUpdateJob?.cancel()
    dragUpdateJob = scope.launch {
        delay(16) // ~60fps
        moveComponent(id, x, y)
    }
}
```

**Solution 2: Local Drag State**
```kotlin
// In Canvas:
var localOffset by remember { mutableStateOf(Offset.Zero) }

Box(
    modifier = Modifier
        .offset { 
            IntOffset(
                (component.xOffset + localOffset.x).roundToInt(),
                (component.yOffset + localOffset.y).roundToInt()
            )
        }
        .pointerInput(component.id) {
            detectDragGestures(
                onDragStart = { localOffset = Offset.Zero },
                onDrag = { change, dragAmount ->
                    localOffset += dragAmount
                },
                onDragEnd = {
                    onComponentMoved(component.id, component.xOffset + localOffset.x, ...)
                    localOffset = Offset.Zero
                }
            )
        }
)
```
- Updates visual position immediately (no state update)
- Only updates StateFlow on drag end
- **Dramatically reduces StateFlow updates**

**Solution 3: Mutable Component Positions**
- Make ComponentInstance a class (not data class)
- Make xOffset/yOffset mutable vars
- Update in-place during drag
- Manually trigger StateFlow update on drag end
- **Most performant, but breaks immutability**

**Recommendation:** Start with simple approach, apply Solution 2 if lag observed

---

## Execution Blockers

### Must Be Resolved Before Starting:

1. **Phase 2 Must Be Complete:**
   - ComponentRegistry.render() must work
   - At least 2-3 components available for testing
   - **Validation:** Can manually call ComponentRegistry.render() successfully

2. **Compose Gestures Available:**
   - Verify detectDragGestures can be imported
   - Verify androidx.compose.foundation.gestures is in dependencies
   - **Current status:** Should be included via compose-foundation

### Can Be Resolved During Execution:

1. Exact canvas bounds values
2. Whether to add selection feedback
3. Performance optimizations

---

## Success Criteria for Phase 3

✅ Phase 3 is complete when:

1. **LayoutState works:**
   - Can add components programmatically
   - Can move components and positions update
   - Can clear all components
   - StateFlow properly triggers recomposition

2. **OrchestratorCanvas renders:**
   - Components appear at correct positions
   - Multiple components render without overlap issues
   - Canvas fills available space

3. **Drag functionality works:**
   - Can drag any component
   - Position updates smoothly during drag
   - Component stays within canvas bounds
   - Dragging one component doesn't affect others

4. **Cross-platform validation:**
   - Works on at least 2 platforms (Desktop + Android recommended)
   - Gestures respond properly on both touch and mouse

5. **Integration ready:**
   - Canvas can be used in App.kt
   - State can be observed and modified externally
   - Ready for Phase 4 (LLM integration) to add components

---

## Phase 3 → Phase 4 Handoff

### What Phase 4 Needs from Phase 3:

1. **LayoutState.addComponent()** - LLM will call this to add components
2. **LayoutState.manifest StateFlow** - For observing current layout
3. **Working canvas** - To visually test LLM-generated components

### Integration Point:
```kotlin
// Phase 4 LLM will do:
val components = intentParser.parseIntent("add a blue button")
components.forEach { component ->
    layoutState.addComponent(component)  // <-- This is the interface
}
```

**Critical:** LayoutState must be stable and thoroughly tested before Phase 4

---

## Time Estimates

### Minimum (Basic Canvas + State):
- LayoutState: 30 min
- OrchestratorCanvas (no drag): 30 min
- Test harness: 15 min
- **Total: 1.25 hours**

### Standard (With Drag):
- LayoutState with validation: 45 min
- OrchestratorCanvas with drag: 1 hour
- Bounds checking: 20 min
- Test harness: 20 min
- Manual testing: 30 min
- **Total: 3 hours**

### Complete (With Polish):
- All standard features: 3 hours
- Selection feedback: 30 min
- Performance optimization: 30 min
- Unit tests: 45 min
- Cross-platform testing: 45 min
- **Total: 5.5 hours**

**Recommendation:** Aim for Standard (3 hours), add polish if time permits

---

## Recommended Execution Strategy

### Option A: Fast Track (1.5 hours)
1. Create LayoutState with basic operations
2. Create OrchestratorCanvas with static positioning (no drag)
3. Create test harness to add/clear components
4. Validate components render at positions
5. **Skip drag for now, add in Phase 5**

### Option B: Standard (Recommended - 3 hours)
1. Create LayoutState with validation and bounds checking
2. Create OrchestratorCanvas with offset positioning
3. Add drag gesture support
4. Create comprehensive test harness
5. Manual testing on one platform
6. Fix any issues found

### Option C: Thorough (5.5 hours)
1. All standard features
2. Add selection feedback
3. Add performance optimizations
4. Write unit tests
5. Test on multiple platforms
6. Polish UX

**Recommendation: Option B (Standard)** - Gets drag working, which is essential for POC

---

## Post-Phase 3 Deliverables

After completing Phase 3, you'll have:

1. **Working interactive canvas** that renders components
2. **Drag-and-drop functionality** for repositioning
3. **State management system** ready for persistence
4. **Visual proof of concept** - can manually create layouts
5. **Foundation for LLM integration** - just need to wire up text input to addComponent()

This is a major milestone - the orchestrator becomes visually tangible and interactive!

---

## Questions to Resolve Before Coding Phase 3

1. **Should we implement removeComponent()?**
   - Recommendation: Yes, essential for fixing mistakes

2. **Canvas bounds values?**
   - Recommendation: 2000x3000 pixels, can adjust later

3. **Selection feedback?**
   - Recommendation: Skip for now unless Phase 3 finishes early

4. **Platform priority for testing?**
   - Recommendation: Desktop (JVM) first (easier to test), then Android

5. **Performance threshold?**
   - Recommendation: Should handle 20 components smoothly, optimize if needed

Ready to proceed with Phase 3 implementation, or want to refine the plan further?

