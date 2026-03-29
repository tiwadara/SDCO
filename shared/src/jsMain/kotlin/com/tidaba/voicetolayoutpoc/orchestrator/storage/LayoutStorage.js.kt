package com.tidaba.voicetolayoutpoc.orchestrator.storage

import com.tidaba.voicetolayoutpoc.orchestrator.models.LayoutManifest
import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json

/**
 * JS implementation: stores layout in browser localStorage.
 */
actual class LayoutStorage actual constructor() {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    actual suspend fun save(manifest: LayoutManifest): Result<Unit> {
        return runCatching {
            val jsonString = json.encodeToString(LayoutManifest.serializer(), manifest)
            localStorage.setItem(STORAGE_KEY, jsonString)
        }
    }

    actual suspend fun load(): Result<LayoutManifest> {
        return runCatching {
            val jsonString = localStorage.getItem(STORAGE_KEY)
                ?: return@runCatching LayoutManifest()
            json.decodeFromString(LayoutManifest.serializer(), jsonString)
        }
    }

    actual suspend fun delete(): Result<Unit> {
        return runCatching {
            localStorage.removeItem(STORAGE_KEY)
        }
    }

    companion object {
        private const val STORAGE_KEY = "voicetolayoutpoc_orchestrator_layout"
    }
}

