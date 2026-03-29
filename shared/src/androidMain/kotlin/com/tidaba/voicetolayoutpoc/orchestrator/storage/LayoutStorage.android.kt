package com.tidaba.voicetolayoutpoc.orchestrator.storage

import com.tidaba.voicetolayoutpoc.orchestrator.models.LayoutManifest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Android implementation: stores layout as a JSON file.
 * Uses a fallback directory since we don't have Context in the shared module.
 * The storage path can be configured via [setStorageDir] from the Android app layer.
 */
actual class LayoutStorage actual constructor() {

    private val mutex = Mutex()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val storageFile: File
        get() {
            val dir = customStorageDir ?: File(System.getProperty("user.home") ?: "/tmp", ".voicetolayoutpoc")
            dir.mkdirs()
            return File(dir, STORAGE_FILE_NAME)
        }

    actual suspend fun save(manifest: LayoutManifest): Result<Unit> = mutex.withLock {
        runCatching {
            val jsonString = json.encodeToString(LayoutManifest.serializer(), manifest)
            storageFile.writeText(jsonString, Charsets.UTF_8)
        }
    }

    actual suspend fun load(): Result<LayoutManifest> = mutex.withLock {
        runCatching {
            if (!storageFile.exists()) {
                return@runCatching LayoutManifest()
            }
            val jsonString = storageFile.readText(Charsets.UTF_8)
            json.decodeFromString(LayoutManifest.serializer(), jsonString)
        }
    }

    actual suspend fun delete(): Result<Unit> = mutex.withLock {
        runCatching {
            if (storageFile.exists()) {
                storageFile.delete()
            }
        }
    }

    companion object {
        private const val STORAGE_FILE_NAME = "orchestrator_layout.json"

        /**
         * Set from Android Application/Activity to use Context.filesDir.
         * Call `LayoutStorage.setStorageDir(context.filesDir)` during app init.
         */
        private var customStorageDir: File? = null

        fun setStorageDir(dir: File) {
            customStorageDir = dir
        }
    }
}

