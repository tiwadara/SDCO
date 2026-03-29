package com.tidaba.voicetolayoutpoc.orchestrator.llm

import com.tidaba.voicetolayoutpoc.orchestrator.DesignSystemSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IntentParserTest {

    // ── extractJson ────────────────────────────────────────────────────

    @Test
    fun testExtractJsonPlainArray() {
        val input = """[{"name":"Button","properties":{"label":"OK"},"xOffset":100,"yOffset":100}]"""
        val result = IntentParser.extractJson(input)
        assertEquals(input, result)
    }

    @Test
    fun testExtractJsonWithMarkdownFence() {
        val input = """
```json
[{"name":"Button","properties":{"label":"OK"},"xOffset":100,"yOffset":100}]
```
        """.trimIndent()
        val result = IntentParser.extractJson(input)
        assertTrue(result.startsWith("["))
        assertTrue(result.endsWith("]"))
        assertTrue(result.contains("Button"))
    }

    @Test
    fun testExtractJsonWithPlainFence() {
        val input = """
```
[{"name":"Text","properties":{"text":"Hello"},"xOffset":50,"yOffset":50}]
```
        """.trimIndent()
        val result = IntentParser.extractJson(input)
        assertTrue(result.startsWith("["))
        assertTrue(result.contains("Text"))
    }

    @Test
    fun testExtractJsonWithLeadingText() {
        val input = """
Here is the JSON output:
[{"name":"Card","properties":{"title":"Welcome"},"xOffset":100,"yOffset":100}]
        """.trimIndent()
        val result = IntentParser.extractJson(input)
        assertTrue(result.startsWith("["))
        assertTrue(result.endsWith("]"))
        assertTrue(result.contains("Card"))
    }

    @Test
    fun testExtractJsonWithTrailingText() {
        val input = """[{"name":"Button","properties":{},"xOffset":100,"yOffset":100}]
Hope that helps!
        """.trimIndent()
        val result = IntentParser.extractJson(input)
        assertTrue(result.startsWith("["))
        assertTrue(result.endsWith("]"))
    }

    @Test
    fun testExtractJsonFailsOnNoArray() {
        assertFailsWith<IllegalStateException> {
            IntentParser.extractJson("I cannot help with that request.")
        }
    }

    @Test
    fun testExtractJsonFailsOnEmptyString() {
        assertFailsWith<IllegalStateException> {
            IntentParser.extractJson("")
        }
    }

    @Test
    fun testExtractJsonMultipleComponents() {
        val input = """
[
  {"name":"TextField","properties":{"label":"Email"},"xOffset":100,"yOffset":100},
  {"name":"TextField","properties":{"label":"Password"},"xOffset":100,"yOffset":180},
  {"name":"Button","properties":{"label":"Login","color":"Primary"},"xOffset":100,"yOffset":260}
]
        """.trimIndent()
        val result = IntentParser.extractJson(input)
        assertTrue(result.contains("Email"))
        assertTrue(result.contains("Password"))
        assertTrue(result.contains("Login"))
    }

    // ── LlmComponentOutput deserialization ──────────────────────────────

    @Test
    fun testDeserializeSingleComponent() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        val input = """[{"name":"Button","properties":{"label":"Submit","color":"Primary"},"xOffset":100,"yOffset":200}]"""
        val parsed = json.decodeFromString<List<LlmComponentOutput>>(input)

        assertEquals(1, parsed.size)
        assertEquals("Button", parsed[0].name)
        assertEquals("Submit", parsed[0].properties["label"])
        assertEquals("Primary", parsed[0].properties["color"])
        assertEquals(100f, parsed[0].xOffset)
        assertEquals(200f, parsed[0].yOffset)
    }

    @Test
    fun testDeserializeMultipleComponents() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        val input = """[
            {"name":"TextField","properties":{"label":"Email"},"xOffset":100,"yOffset":100},
            {"name":"Button","properties":{"label":"Go"},"xOffset":100,"yOffset":180}
        ]"""
        val parsed = json.decodeFromString<List<LlmComponentOutput>>(input)

        assertEquals(2, parsed.size)
        assertEquals("TextField", parsed[0].name)
        assertEquals("Button", parsed[1].name)
    }

    @Test
    fun testDeserializeWithMissingOptionalFields() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        val input = """[{"name":"Card"}]"""
        val parsed = json.decodeFromString<List<LlmComponentOutput>>(input)

        assertEquals(1, parsed.size)
        assertEquals("Card", parsed[0].name)
        assertEquals(emptyMap(), parsed[0].properties)
        assertEquals(100f, parsed[0].xOffset)
        assertEquals(100f, parsed[0].yOffset)
    }

    @Test
    fun testDeserializeIgnoresExtraKeys() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        val input = """[{"name":"Button","properties":{"label":"OK"},"xOffset":50,"yOffset":50,"unknownField":"ignored"}]"""
        val parsed = json.decodeFromString<List<LlmComponentOutput>>(input)

        assertEquals(1, parsed.size)
        assertEquals("Button", parsed[0].name)
    }
}

class PromptBuilderTest {

    @Test
    fun testPromptContainsUserInput() {
        val prompt = PromptBuilder.buildPrompt("add a blue button")
        assertTrue(prompt.contains("add a blue button"))
    }

    @Test
    fun testPromptContainsComponentNames() {
        val prompt = PromptBuilder.buildPrompt("test")
        DesignSystemSchema.componentNames.forEach { name ->
            assertTrue(prompt.contains(name), "Prompt should contain component '$name'")
        }
    }

    @Test
    fun testPromptContainsColorTokens() {
        val prompt = PromptBuilder.buildPrompt("test")
        assertTrue(prompt.contains("Primary"), "Prompt should mention Primary color")
        assertTrue(prompt.contains("Error"), "Prompt should mention Error color")
    }

    @Test
    fun testPromptContainsJsonExample() {
        val prompt = PromptBuilder.buildPrompt("test")
        assertTrue(prompt.contains("xOffset"), "Prompt should show xOffset in format")
        assertTrue(prompt.contains("yOffset"), "Prompt should show yOffset in format")
        assertTrue(prompt.contains("properties"), "Prompt should show properties in format")
    }

    @Test
    fun testPromptContainsPositioningRules() {
        val prompt = PromptBuilder.buildPrompt("test")
        assertTrue(prompt.contains("2000"), "Prompt should mention canvas width")
        assertTrue(prompt.contains("3000"), "Prompt should mention canvas height")
    }
}

