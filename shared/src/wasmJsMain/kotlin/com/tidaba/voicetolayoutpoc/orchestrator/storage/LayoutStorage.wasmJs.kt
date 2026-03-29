package com.tidaba.voicetolayoutpoc.orchestrator.storage

import com.tidaba.voicetolayoutpoc.orchestrator.models.LayoutManifest
import kotlinx.serialization.json.Json

/**
 * WasmJS implementation: in-memory storage for POC.
 * TODO: Implement localStorage via JS interop when WasmJS APIs stabilize.
 */
actual class LayoutStorage actual constructor() {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var cached: String? = null

    actual suspend fun save(manifest: LayoutManifest): Result<Unit> {
        return runCatching {
            cached = json.encodeToString(LayoutManifest.serializer(), manifest)
        }
    }

    actual suspend fun load(): Result<LayoutManifest> {
        return runCatching {
            val jsonString = cached ?: return@runCatching LayoutManifest()
            json.decodeFromString(LayoutManifest.serializer(), jsonString)
        }
    }

    actual suspend fun delete(): Result<Unit> {
        return runCatching {
            cached = null
        }
    }
}

