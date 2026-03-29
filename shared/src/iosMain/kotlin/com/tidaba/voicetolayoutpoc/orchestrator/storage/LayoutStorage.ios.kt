package com.tidaba.voicetolayoutpoc.orchestrator.storage

import com.tidaba.voicetolayoutpoc.orchestrator.models.LayoutManifest
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile

/**
 * iOS implementation: stores layout as a JSON file in the app's Documents directory.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class LayoutStorage actual constructor() {

    private val mutex = Mutex()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val storagePath: String by lazy {
        val paths = NSFileManager.defaultManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        )
        @Suppress("UNCHECKED_CAST")
        val documentsUrl = paths.first() as platform.Foundation.NSURL
        val documentsPath = documentsUrl.path ?: ""
        (documentsPath as NSString).stringByAppendingPathComponent(STORAGE_FILE_NAME)
    }

    actual suspend fun save(manifest: LayoutManifest): Result<Unit> = mutex.withLock {
        runCatching {
            val jsonString = json.encodeToString(LayoutManifest.serializer(), manifest)
            val nsString = NSString.create(string = jsonString)
            nsString.writeToFile(storagePath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
            Unit
        }
    }

    actual suspend fun load(): Result<LayoutManifest> = mutex.withLock {
        runCatching {
            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(storagePath)) {
                return@runCatching LayoutManifest()
            }
            val nsString = NSString.create(contentsOfFile = storagePath, encoding = NSUTF8StringEncoding, error = null)
                ?: return@runCatching LayoutManifest()
            val jsonString = nsString as String
            json.decodeFromString(LayoutManifest.serializer(), jsonString)
        }
    }

    actual suspend fun delete(): Result<Unit> = mutex.withLock {
        runCatching {
            val fileManager = NSFileManager.defaultManager
            if (fileManager.fileExistsAtPath(storagePath)) {
                fileManager.removeItemAtPath(storagePath, error = null)
            }
            Unit
        }
    }

    companion object {
        private const val STORAGE_FILE_NAME = "orchestrator_layout.json"
    }
}


