# Phase 2 Execution Analysis: Component Library (Material 3 Wrappers)

## Overview

Phase 2 creates the "vocabulary" of UI components that can be instantiated via voice/text commands. We're wrapping Material 3 components with a standardized property-based interface.

## Current State

### ✅ Prerequisites from Phase 1
- ComponentInstance data model exists (with name, properties map)
- DesignSystemSchema defines available component names
- Compose Material 3 dependencies already added

### 📋 What Phase 2 Delivers
- 5 wrapped Material 3 components that accept `Map<String, String>` properties
- ComponentRegistry that maps string names to Composable functions
- Foundation for LLM to create any component by name

---

## Step 2.1: Create Reusable Components

### Files to Create:
```
shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/
└── components/
    ├── OrchestratorButton.kt
    ├── OrchestratorCard.kt
    ├── OrchestratorTextField.kt
    ├── OrchestratorText.kt
    └── OrchestratorCheckbox.kt
```

---

### Analysis: OrchestratorButton.kt

**Purpose:** Wraps Material 3 Button with dynamic properties

**Supported Properties:**
- `label` - Button text (default: "Button")
- `color` - Not directly mappable to Material (needs color scheme interpretation)
- `variant` - Could support: "filled" (default), "outlined", "text"

**Key Decisions:**

1. **onClick Behavior:**
   - Currently: `onClick = {}` (no-op)
   - **Problem:** User can click but nothing happens
   - **Options:**
     - Keep no-op for POC (simplest)
     - Add "action" property with predefined actions ("log", "navigate", etc.)
     - Emit events through callback
   - **Recommendation:** No-op for POC, document as limitation

2. **Color Interpretation:**
   ```kotlin
   // User says "blue button" → properties["color"] = "blue"
   // Material 3 uses ColorScheme, not direct colors
   ```
   - **Challenge:** Map "blue" → MaterialTheme.colorScheme.primary
   - **Options:**
     - Ignore color for POC, always use primary
     - Create color mapper: "blue" → primary, "red" → error, "gray" → secondary
     - Use containerColor parameter
   - **Recommendation:** Simple mapper for POC

3. **Button Variants:**
   Material 3 has: Button, OutlinedButton, TextButton, FilledTonalButton
   - Should properties["variant"] switch between these?
   - **Recommendation:** Start with Button only, one variant

**Implementation Complexity:** 🟢 Low (15 minutes)

**Potential Code:**
```kotlin
@Composable
fun OrchestratorButton(properties: Map<String, String>) {
    val label = properties["label"] ?: "Button"
    val colorName = properties["color"]
    
    // Simple color mapping
    val buttonColors = when (colorName) {
        "blue", "primary" -> ButtonDefaults.buttonColors()
        "red", "error" -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
        else -> ButtonDefaults.buttonColors()
    }
    
    Button(
        onClick = { /* No-op for POC */ },
        colors = buttonColors
    ) {
        Text(label)
    }
}
```

---

### Analysis: OrchestratorCard.kt

**Purpose:** Wraps Material 3 Card to display content containers

**Supported Properties:**
- `title` - Main heading (default: "Card Title")
- `subtitle` - Secondary text (default: "Subtitle")
- `content` - Body text (optional)

**Key Decisions:**

1. **Card Structure:**
   - Plan shows: Column with title + subtitle
   - **Question:** Should we support more complex layouts inside cards?
   - **Recommendation:** Keep simple for POC, just text content

2. **Card Variants:**
   Material 3 has: Card, ElevatedCard, OutlinedCard
   - **Recommendation:** Use ElevatedCard (looks better, more visible)

3. **Fixed vs Dynamic Height:**
   - Current plan: Content wraps
   - **Issue:** Cards could be vastly different sizes
   - **Recommendation:** Add `modifier = Modifier.width(200.dp)` for consistency

**Implementation Complexity:** 🟢 Low (15 minutes)

**Potential Issue:**
- If user says "add a card" without title/subtitle, it looks empty
- **Solution:** Use meaningful defaults or generate placeholder text

---

### Analysis: OrchestratorTextField.kt

**Purpose:** Text input field with dynamic label

**Supported Properties:**
- `label` - Field label (default: "Input")
- `placeholder` - Hint text (optional)

**Key Decisions:**

1. **State Management Problem:**
   ```kotlin
   var text by remember { mutableStateOf("") }
   ```
   - **Issue:** Each TextField has isolated state
   - If user drags/recreates component, state is lost
   - **Options:**
     - Accept this limitation for POC
     - Store text value in ComponentInstance.properties
     - Add separate state management layer
   - **Recommendation:** Accept limitation, document as known issue

2. **TextField vs OutlinedTextField:**
   - OutlinedTextField is more common in modern apps
   - **Recommendation:** Use OutlinedTextField

3. **Keyboard Type:**
   - Could support properties["type"] = "number", "email", "password"
   - **Recommendation:** Defer to later, all text for POC

**Implementation Complexity:** 🟢 Low (10 minutes)

**Known Limitation:** Text entered by user is not persisted (state lost on drag/recomposition)

---

### Analysis: OrchestratorText.kt

**Purpose:** Display styled text labels/headings

**Supported Properties:**
- `text` - The actual text content (default: "Text")
- `style` - Typography style: "headline", "body", "label" (default: "body")

**Key Decisions:**

1. **Typography Mapping:**
   Material 3 typography scale:
   - displayLarge, displayMedium, displaySmall
   - headlineLarge, headlineMedium, headlineSmall
   - titleLarge, titleMedium, titleSmall
   - bodyLarge, bodyMedium, bodySmall
   - labelLarge, labelMedium, labelSmall
   
   **Simplified mapping for LLM:**
   - "headline" → headlineMedium
   - "title" → titleLarge
   - "body" → bodyLarge
   - "caption" → labelSmall
   - **Recommendation:** Use simplified names, easier for LLM

2. **Text Color:**
   - Should properties["color"] override text color?
   - **Recommendation:** Yes, but use MaterialTheme colors only

3. **Text Wrapping:**
   - Long text might overflow
   - **Recommendation:** Add `modifier = Modifier.widthIn(max = 300.dp)` for safety

**Implementation Complexity:** 🟢 Low (10 minutes)

---

### Analysis: OrchestratorCheckbox.kt

**Purpose:** Checkbox with label

**Supported Properties:**
- `label` - Text next to checkbox (default: "Option")
- `checked` - Initial state: "true"/"false" (default: "false")

**Key Decisions:**

1. **State Management (Same as TextField):**
   ```kotlin
   var checked by remember { mutableStateOf(false) }
   ```
   - **Issue:** State not persisted in ComponentInstance
   - **Recommendation:** Accept limitation for POC

2. **Layout:**
   - Checkbox + Text in Row
   - Or just Checkbox alone?
   - **Recommendation:** Row with label (more useful)

**Implementation Complexity:** 🟢 Low (10 minutes)

**Known Limitation:** Checked state not persisted

---

## Step 2.2: Component Registry

### File to Create:
```
shared/src/commonMain/kotlin/com/tidaba/voicetolayoutpoc/orchestrator/
└── registry/
    └── ComponentRegistry.kt
```

### Analysis: ComponentRegistry.kt

**Purpose:** Central lookup table mapping component names to Composable functions

**Architecture:**

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

**Key Decisions:**

1. **Registry Pattern Choice:**
   - **Current:** Simple `when` expression
   - **Alternative:** `Map<String, @Composable (Map<String, String>) -> Unit>`
   ```kotlin
   private val registry = mapOf(
       "Button" to { props -> OrchestratorButton(props) }
   )
   ```
   - **Trade-off:** Map is more flexible but `when` is simpler and more readable
   - **Recommendation:** Keep `when` for POC

2. **Case Sensitivity:**
   - Should "button", "Button", "BUTTON" all work?
   - **Issue:** LLM might return inconsistent casing
   - **Solution:** `component.name.lowercase()` in when expression
   - **Recommendation:** Normalize to lowercase

3. **Unknown Component Handling:**
   - Currently shows: `Text("Unknown: ${component.name}")`
   - **Better:** Show styled error card with component name?
   - **Recommendation:** Keep simple text for debugging

4. **Registry Validation:**
   - Should we validate that DesignSystemSchema.availableComponents matches registry keys?
   - **Recommendation:** Add validation function:
   ```kotlin
   fun validate(): Boolean {
       val registered = getAvailableComponents()
       val schema = DesignSystemSchema.tokens.availableComponents
       return registered.toSet() == schema.toSet()
   }
   ```

**Implementation Complexity:** 🟢 Low (20 minutes including validation)

---

## Cross-Component Design Challenges

### Challenge 1: Consistent Sizing

**Problem:** Components have vastly different default sizes
- Button: Wraps content (small)
- Card: Could be any size
- TextField: Full width by default
- Text: Wraps content
- Checkbox: Small icon + text

**Impact on Canvas:**
- Components might overlap unexpectedly
- Hard to position accurately with drag-and-drop
- Visual inconsistency

**Solutions:**
1. **Minimum Bounds:** All components get `Modifier.defaultMinSize(minWidth = 120.dp, minHeight = 48.dp)`
2. **Fixed Width:** All get `.width(200.dp)` except Text
3. **Size Properties:** Add `width`, `height` to properties map
4. **Recommendation:** Use Solution 1 (minimum bounds) - prevents tiny components while allowing natural sizing

### Challenge 2: Interactive State Management

**Problem:** TextField and Checkbox have internal state that's lost when:
- Component is dragged (recomposed with new offset)
- Layout is saved/loaded
- User adds similar component

**Options:**
1. **Accept Loss (POC approach):**
   - These are "presentational" components
   - User can see them but state resets
   - Document as limitation

2. **Add State to ComponentInstance:**
   ```kotlin
   properties = {
       "label": "Email",
       "value": "user@example.com"  // <-- Store actual text
   }
   ```
   - Need to lift state up to LayoutState
   - Update properties on every keystroke
   - Complex state synchronization

3. **Separate State Layer:**
   - ComponentInstance for structure
   - ComponentState for runtime values
   - Keep them in sync
   - Most complex but most correct

**Recommendation:** Option 1 for Phase 2, upgrade to Option 2 in later phase

### Challenge 3: Property Key Naming Convention

**Problem:** Need consistent property names for LLM to use

**Examples:**
- Button: "label" or "text"?
- Card: "title" + "subtitle" or "heading" + "body"?
- Text: "text" or "content"?

**Recommendation:** Create property naming guide:
```
Button:
  - label: string (button text)
  - color: string (blue|red|green|primary|secondary|error)
  - variant: string (filled|outlined|text) [future]

Card:
  - title: string (main heading)
  - subtitle: string (secondary text)
  - content: string (body text) [optional]

TextField:
  - label: string (field label)
  - placeholder: string (hint text)
  - value: string (initial value) [not implemented in POC]

Text:
  - text: string (content to display)
  - style: string (headline|title|body|caption)
  - color: string (color name)

Checkbox:
  - label: string (text next to checkbox)
  - checked: string ("true"|"false")
```

This should be documented in DesignSystemSchema for LLM prompt

---

## Implementation Dependencies

### Internal Dependencies (must be completed first):
- ✅ ComponentInstance.kt (from Phase 1)
- ✅ MaterialTheme available (already in dependencies)

### External Dependencies:
- ⚠️ Need to verify Compose imports work in shared module
- Material 3 components: Button, Card, TextField, Text, Checkbox
- Compose modifiers: padding, width, defaultMinSize

### Potential Import Issues:

**Compose UI imports in shared module:**
```kotlin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
```

**Risk:** Shared module might not resolve Compose imports on all platforms
- **Mitigation:** Build.gradle.kts already has compose dependencies
- **Validation:** Try importing in a test file first

---

## Execution Order & Time Estimates

### Sequential Approach (Recommended):
```
1. Create directory structure (2 min)
   └── shared/src/commonMain/kotlin/.../orchestrator/components/

2. Build simplest component first: OrchestratorText.kt (10 min)
   └── Test: Can we import Compose? Does it compile?
   └── If fails: Debug dependency issues before continuing

3. Build OrchestratorButton.kt (15 min)
   └── Add color mapping logic
   └── Test different property combinations

4. Build OrchestratorCheckbox.kt (10 min)
   └── Simple Row layout

5. Build OrchestratorTextField.kt (10 min)
   └── Document state limitation

6. Build OrchestratorCard.kt (15 min)
   └── Test with long title/subtitle

7. Create ComponentRegistry.kt (20 min)
   └── Add all 5 components
   └── Add case-insensitive matching
   └── Add validation function

8. Write property naming documentation (10 min)
   └── Update DesignSystemSchema with property specs

Total: ~90 minutes (1.5 hours)
```

### Parallel Approach (If multiple developers):
- Developer A: Button + Text components
- Developer B: TextField + Checkbox components  
- Developer C: Card + ComponentRegistry

---

## Testing Strategy

### Unit Testing (Difficult):
- Composables are hard to unit test without UI testing framework
- **Recommendation:** Skip unit tests for components, test via UI

### Manual Testing Checklist:

Create a temporary test screen that renders each component:

```kotlin
// In composeApp for testing
@Composable
fun ComponentLibraryTestScreen() {
    Column(modifier = Modifier.padding(16.dp).verticalScroll()) {
        Text("Component Library Test", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Test Button
        OrchestratorButton(mapOf("label" to "Submit"))
        OrchestratorButton(mapOf("label" to "Cancel", "color" to "red"))
        
        // Test Card
        OrchestratorCard(mapOf("title" to "Card Title", "subtitle" to "Card subtitle text"))
        
        // Test TextField
        OrchestratorTextField(mapOf("label" to "Email"))
        
        // Test Text
        OrchestratorText(mapOf("text" to "This is body text", "style" to "body"))
        OrchestratorText(mapOf("text" to "This is a headline", "style" to "headline"))
        
        // Test Checkbox
        OrchestratorCheckbox(mapOf("label" to "Accept terms"))
    }
}
```

**Test Cases:**
1. ✅ All 5 components render without crash
2. ✅ Properties are applied (labels show correct text)
3. ✅ Default values work (render with empty map)
4. ✅ Components are visually distinguishable
5. ✅ Interactive components respond to user input (checkbox toggles, textfield accepts input)

### Integration Testing:

Test ComponentRegistry:
```kotlin
// Should render button
ComponentRegistry.render(ComponentInstance(
    id = "test1",
    name = "Button",
    properties = mapOf("label" to "Test")
))

// Should handle unknown component gracefully
ComponentRegistry.render(ComponentInstance(
    id = "test2", 
    name = "UnknownWidget",
    properties = emptyMap()
))
```

---

## Risk Assessment

### 🟢 Low Risk Items:
- **Component implementation** - Straightforward wrappers
- **Registry pattern** - Simple when statement
- **Material 3 compatibility** - Well-established library

### 🟡 Medium Risk Items:

1. **Compose in Shared Module:**
   - **Risk:** Shared module might not have proper Compose setup
   - **Mitigation:** Dependencies are added, should work
   - **Validation:** Test imports immediately

2. **Component Sizing Consistency:**
   - **Risk:** UI looks messy with different sized components
   - **Mitigation:** Apply minimum bounds modifier
   - **Impact:** Medium (affects UX but not functionality)

3. **Property Parsing Ambiguity:**
   - **Risk:** LLM uses "text" vs "label" inconsistently
   - **Mitigation:** Clear documentation in prompt
   - **Impact:** Low (components have defaults)

### 🔴 High Risk Items:

1. **State Management for Interactive Components:**
   - **Risk:** User types in TextField, drags it, text disappears
   - **Mitigation:** Document as known limitation
   - **Impact:** High for user experience, but acceptable for POC
   - **Future Fix:** Lift state to LayoutState in Phase 5

2. **Component Preview/Testing:**
   - **Risk:** No easy way to test components in isolation before Phase 3
   - **Mitigation:** Create temporary test screen in composeApp
   - **Impact:** Medium (slows development if issues found late)

---

## Execution Blockers

### Must Be Resolved Before Starting:

1. **Verify Compose works in shared module:**
   - Create simple test file with Material imports
   - Build project
   - Fix any dependency issues

2. **Decide on property naming:**
   - Need standardized keys before building components
   - Affects both component implementation and LLM prompts

### Can Be Resolved During Execution:

1. Color mapping strategy
2. Component sizing approach
3. State management trade-offs

---

## Optimization Opportunities

### 1. Component Base Class (Optional)
Instead of 5 separate files, could create:
```kotlin
sealed class OrchestratorComponent {
    abstract val defaultProperties: Map<String, String>
    
    @Composable
    abstract fun Render(properties: Map<String, String>)
}
```
**Trade-off:** More abstraction vs simpler individual files
**Recommendation:** Skip for POC, individual files are clearer

### 2. Property Validation (Optional)
```kotlin
data class ButtonProperties(val label: String, val color: String?) {
    companion object {
        fun fromMap(map: Map<String, String>) = ButtonProperties(
            label = map["label"] ?: "Button",
            color = map["color"]
        )
    }
}
```
**Trade-off:** Type safety vs flexibility
**Recommendation:** Defer to later phase

### 3. Component Presets (Optional)
```kotlin
object ComponentPresets {
    val submitButton = mapOf("label" to "Submit", "color" to "primary")
    val cancelButton = mapOf("label" to "Cancel", "color" to "secondary")
}
```
**Benefit:** LLM can reference presets
**Recommendation:** Interesting but defer to later

---

## Success Criteria for Phase 2

✅ Phase 2 is complete when:

1. **All 5 component files compile** without errors
2. **ComponentRegistry renders all components** by name lookup
3. **Components are visually testable** (can see them in test screen)
4. **Property system works:**
   - Can pass properties via map
   - Defaults apply when properties missing
   - At least 2 properties per component work (e.g., label + color)
5. **Documentation exists** for supported properties (for Phase 4 LLM prompts)

---

## Phase 2 → Phase 3 Handoff

### What Phase 3 Needs from Phase 2:

1. **ComponentRegistry.render()** - Must work reliably
2. **Component sizing** - Should be predictable for canvas layout
3. **Property documentation** - For testing canvas with hardcoded components

### Integration Point:
```kotlin
// Phase 3 canvas will call:
Box(modifier = Modifier.offset { ... }) {
    ComponentRegistry.render(component)  // <-- This is the interface
}
```

**Critical:** ComponentRegistry.render() must be stable and tested before Phase 3

---

## Recommended Execution Strategy

### Option A: Fast Track (1.5 hours)
1. Create all 5 component files with minimal properties
2. Create ComponentRegistry with basic when statement
3. Create test screen in composeApp
4. Validate all components render
5. Move to Phase 3

### Option B: Thorough (3 hours)
1. Create components one at a time
2. Test each component individually
3. Add color mapping logic
4. Add sizing constraints
5. Write property documentation
6. Create ComponentRegistry with validation
7. Comprehensive manual testing

### Option C: Iterative (Recommended - 2 hours)
1. Create Text component (simplest) + test
2. Create Button component + color mapping + test
3. Create remaining 3 components
4. Create ComponentRegistry
5. Test all together in test screen
6. Document properties while testing

**Recommendation: Option C** - Good balance of speed and validation

---

## Dependency on Phase 1

Phase 2 **can start in parallel** with Phase 1 Part D (storage implementations):
- Components only need ComponentInstance data model (Part A)
- Don't need actual storage working yet
- Can test components independently

**Optimization:** Start Phase 2 while Phase 1 storage is being built for different platforms

---

## Post-Phase 2 Deliverables

After completing Phase 2, you'll have:

1. **5 working Material 3 component wrappers** in shared module
2. **ComponentRegistry** that can render any component by name
3. **Property naming conventions** documented
4. **Test screen** showing all components work
5. **Clear interface** for Phase 3 canvas to use

This sets up everything needed for the canvas in Phase 3, where components will be positioned and dragged around.

---

## Questions to Resolve Before Coding Phase 2

1. **Should we test Phase 2 in isolation** with a dedicated test screen, or wait until Phase 3 canvas is built?
   - Recommendation: Test screen in composeApp (faster feedback)

2. **How much effort on color/styling?** Basic defaults vs full color support?
   - Recommendation: Basic defaults + simple color mapper (5-6 colors)

3. **Do we need all 5 components immediately** or start with 2-3?
   - Recommendation: Build all 5 (only 90 min total, good coverage)

Ready to proceed with Phase 2, or want to resolve any decisions first?

