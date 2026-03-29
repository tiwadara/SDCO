package com.tidaba.voicetolayoutpoc.orchestrator.storage

import com.tidaba.voicetolayoutpoc.orchestrator.models.LayoutManifest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

/**
 * JVM implementation: stores layout as a JSON file in the user's home directory.
 */
actual class LayoutStorage actual constructor() {

    private val mutex = Mutex()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val storageDir: File by lazy {
        val dir = File(System.getProperty("user.home"), ".voicetolayoutpoc")
        dir.mkdirs()
        dir
    }

    private val storageFile: File
        get() = File(storageDir, STORAGE_FILE_NAME)

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
    }
}

