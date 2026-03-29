package com.tidaba.voicetolayoutpoc.orchestrator.llm

import com.tidaba.voicetolayoutpoc.orchestrator.models.ComponentInstance
import kotlinx.serialization.json.Json

/**
 * Converts natural-language user input into a list of [ComponentInstance]
 * objects by calling the Gemini LLM via [GeminiClient] and parsing the
 * structured JSON response.
 */
class IntentParser(private val geminiClient: GeminiClient) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Send [userInput] to the LLM, parse the JSON response, and return
     * a list of [ComponentInstance] ready to add to the canvas.
     *
     * Each returned instance gets a fresh unique ID (the LLM doesn't
     * generate IDs).
     */
    suspend fun parseIntent(userInput: String): Result<List<ComponentInstance>> {
        val prompt = PromptBuilder.buildPrompt(userInput)

        return geminiClient.generateContent(prompt)
            .mapCatching { rawResponse ->
                val cleaned = extractJson(rawResponse)
                val parsed = json.decodeFromString<List<LlmComponentOutput>>(cleaned)

                parsed.map { output ->
                    ComponentInstance(
                        // Let the default ID generator create unique IDs
                        name = output.name,
                        properties = output.properties,
                        xOffset = output.xOffset,
                        yOffset = output.yOffset,
                    )
                }
            }
    }

    companion object {
        /**
         * Extract a JSON array from the LLM response, stripping markdown
         * code fences or surrounding whitespace.
         */
        internal fun extractJson(response: String): String {
            var cleaned = response.trim()

            // Strip ```json ... ``` or ``` ... ```
            if (cleaned.startsWith("```")) {
                // Remove opening fence (with optional language tag)
                cleaned = cleaned.removePrefix("```json")
                    .removePrefix("```JSON")
                    .removePrefix("```")
                    .trimStart()
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.removeSuffix("```").trimEnd()
            }

            // Ensure we have a JSON array
            val startIndex = cleaned.indexOf('[')
            val endIndex = cleaned.lastIndexOf(']')

            if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
                error("No valid JSON array found in LLM response: ${cleaned.take(200)}")
            }

            return cleaned.substring(startIndex, endIndex + 1)
        }
    }
}

/**
 * Intermediate DTO matching the shape the LLM outputs.
 * We map this to [ComponentInstance] (which has auto-generated ID + extras).
 */
@kotlinx.serialization.Serializable
internal data class LlmComponentOutput(
    val name: String,
    val properties: Map<String, String> = emptyMap(),
    val xOffset: Float = 100f,
    val yOffset: Float = 100f,
)

