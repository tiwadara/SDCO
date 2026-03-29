package com.tidaba.voicetolayoutpoc.orchestrator.llm

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lightweight Ktor-based client for Google's Gemini generateContent REST API.
 *
 * Uses the v1beta endpoint with an API-key query parameter (no OAuth needed).
 *
 * @param apiKey Google AI Studio API key
 * @param model  Model name (default: gemini-2.0-flash)
 */
class GeminiClient(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
) {
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }

    /**
     * Send a prompt to Gemini and return the raw text response.
     * Uses `responseMimeType: application/json` to force structured JSON output.
     */
    suspend fun generateContent(prompt: String): Result<String> = runCatching {
        val requestBody = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
            ),
        )

        val url = "$BASE_URL/$model:generateContent?key=$apiKey"

        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val responseText = response.bodyAsText()

        if (response.status.value !in 200..299) {
            error("Gemini API error ${response.status.value}: $responseText")
        }

        val geminiResponse = jsonConfig.decodeFromString(GeminiResponse.serializer(), responseText)

        geminiResponse.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?: error("Empty response from Gemini")
    }

    /**
     * List models available to this API key.
     * Returns model IDs that support generateContent (e.g. "gemini-1.5-flash").
     */
    suspend fun listModels(): Result<List<String>> = runCatching {
        val url = "$BASE_URL?key=$apiKey"
        val response = client.get(url)
        val responseText = response.bodyAsText()

        if (response.status.value !in 200..299) {
            error("Gemini listModels error ${response.status.value}: $responseText")
        }

        val listResponse = jsonConfig.decodeFromString(ListModelsResponse.serializer(), responseText)

        listResponse.models
            .filter { model ->
                model.supportedGenerationMethods?.contains("generateContent") == true
            }
            .mapNotNull { it.name?.removePrefix("models/") }
            .sorted()
    }

    fun close() {
        client.close()
    }

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val DEFAULT_MODEL = "gemini-1.5-flash"

        private val staticJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        /**
         * Fetch available model names for an API key without creating a full client.
         *
         * Filters to only models useful for our use case (structured JSON generation):
         * - Must support `generateContent`
         * - Must be a `gemini` model (not embedding, AQA, bison, etc.)
         * - Excludes `nano` models (too small for reliable JSON output)
         * - Excludes vision-only / image-generation models
         */
        suspend fun fetchAvailableModels(apiKey: String): Result<List<String>> = runCatching {
            val client = HttpClient {
                install(ContentNegotiation) {
                    json(staticJson)
                }
            }
            try {
                val url = "$BASE_URL?key=$apiKey"
                val response = client.get(url)
                val responseText = response.bodyAsText()

                if (response.status.value !in 200..299) {
                    error("Gemini listModels error ${response.status.value}: $responseText")
                }

                val listResponse = staticJson.decodeFromString(ListModelsResponse.serializer(), responseText)

                val excludePatterns = listOf(
                    "nano",          // too small for structured JSON
                    "embedding",     // embedding models, not generative
                    "aqa",           // attributed QA, not general purpose
                    "bison",         // legacy PaLM models
                    "imagen",        // image generation
                    "veo",           // video generation
                    "lyria",         // music generation
                    "chirp",         // speech models
                    "modeltunning",  // tuning endpoints
                    "learnlm",      // learning models
                )

                listResponse.models
                    .filter { model ->
                        val id = model.name?.removePrefix("models/")?.lowercase() ?: ""
                        // Must support generateContent
                        model.supportedGenerationMethods?.contains("generateContent") == true
                            // Must be a gemini model
                            && id.startsWith("gemini")
                            // Exclude unhelpful model types
                            && excludePatterns.none { pattern -> id.contains(pattern) }
                    }
                    .mapNotNull { it.name?.removePrefix("models/") }
                    .sorted()
            } finally {
                client.close()
            }
        }
    }
}

// ── Gemini REST API DTOs ───────────────────────────────────────────────

@Serializable
internal data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
)

@Serializable
internal data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null,
)

@Serializable
internal data class Content(
    val parts: List<Part>,
    val role: String? = null,
)

@Serializable
internal data class Part(
    val text: String,
)

@Serializable
internal data class GeminiResponse(
    val candidates: List<Candidate>? = null,
)

@Serializable
internal data class Candidate(
    val content: Content? = null,
)

@Serializable
internal data class ListModelsResponse(
    val models: List<ModelInfo> = emptyList(),
)

@Serializable
internal data class ModelInfo(
    val name: String? = null,
    val displayName: String? = null,
    val supportedGenerationMethods: List<String>? = null,
)
