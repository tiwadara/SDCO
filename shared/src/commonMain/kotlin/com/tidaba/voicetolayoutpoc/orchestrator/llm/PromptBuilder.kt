package com.tidaba.voicetolayoutpoc.orchestrator.llm

import com.tidaba.voicetolayoutpoc.orchestrator.DesignSystemSchema

/**
 * Builds the LLM prompt that converts natural-language commands into a
 * JSON array of [ComponentInstance] objects.
 *
 * The prompt is dynamically generated from [DesignSystemSchema] so it
 * always stays in sync with the available components and tokens.
 */
object PromptBuilder {

    /**
     * Build the full prompt including system instructions + user request.
     */
    fun buildPrompt(userInput: String): String {
        return """
$SYSTEM_INSTRUCTIONS

$COMPONENT_CATALOG

$COLOR_TOKENS

$POSITIONING_RULES

$OUTPUT_FORMAT

$EXAMPLES

User request: "$userInput"

Now convert the user request to JSON:
""".trimIndent()
    }

    // ── Prompt Sections ────────────────────────────────────────────────

    private val SYSTEM_INSTRUCTIONS = """
You are a UI Layout Assistant. Your ONLY job is to convert user requests
into a JSON array of UI components. Output NOTHING except valid JSON.
Do NOT wrap in markdown code fences. Do NOT add commentary.
    """.trimIndent()

    private val COMPONENT_CATALOG: String
        get() {
            val lines = DesignSystemSchema.components.joinToString("\n") { comp ->
                val props = comp.editableProperties.joinToString(", ")
                "  - ${comp.name} (${comp.category}): editable properties = [$props]"
            }
            return """
Available components:
$lines
            """.trimIndent()
        }

    private val COLOR_TOKENS: String
        get() {
            val colors = DesignSystemSchema.tokens.colors.keys.joinToString(", ")
            return """
Available colors: $colors
Map friendly color names to the closest match (e.g. "blue" → "Primary", "red" → "Error").
            """.trimIndent()
        }

    private val POSITIONING_RULES = """
Positioning rules:
- The canvas is 2000×3000 pixels.
- If the user does NOT specify a position, start at (100, 100).
- Space multiple components 80px apart vertically.
- "top" → yOffset ~50,  "middle"/"center" → yOffset ~400, "bottom" → yOffset ~700.
- "left" → xOffset ~50,  "center" → xOffset ~200, "right" → xOffset ~400.
    """.trimIndent()

    private val OUTPUT_FORMAT = """
Output format — a JSON array where each element has:
{
  "name": "<ComponentName>",
  "properties": { <key>: <value>, ... },
  "xOffset": <number>,
  "yOffset": <number>
}

Only include properties that the user explicitly or implicitly requests.
Use the component's default values for anything not specified.
    """.trimIndent()

    private val EXAMPLES = """
Examples:

User: "Add a blue button labeled Submit"
Output:
[{"name":"Button","properties":{"label":"Submit","color":"Primary"},"xOffset":100,"yOffset":100}]

User: "Add a card with title Welcome and a text field for email"
Output:
[{"name":"Card","properties":{"title":"Welcome","subtitle":""},"xOffset":100,"yOffset":100},{"name":"TextField","properties":{"label":"Email","placeholder":"Enter email"},"xOffset":100,"yOffset":180}]

User: "Create a login form with email field, password field, and submit button"
Output:
[{"name":"TextField","properties":{"label":"Email","placeholder":"Enter email"},"xOffset":100,"yOffset":100},{"name":"TextField","properties":{"label":"Password","placeholder":"Enter password"},"xOffset":100,"yOffset":180},{"name":"Button","properties":{"label":"Submit","color":"Primary"},"xOffset":100,"yOffset":260}]
    """.trimIndent()
}

