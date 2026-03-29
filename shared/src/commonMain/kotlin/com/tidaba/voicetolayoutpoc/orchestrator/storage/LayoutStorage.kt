package com.tidaba.voicetolayoutpoc.orchestrator.storage

import com.tidaba.voicetolayoutpoc.orchestrator.models.LayoutManifest

/**
 * Platform-agnostic storage interface for layout persistence.
 *
 * Each platform provides its own [LayoutStorage] implementation via expect/actual.
 * - JVM: File-based (user home directory)
 * - Android: File-based (app internal storage)
 * - iOS: File-based (documents directory)
 * - JS/WasmJS: localStorage
 */
expect class LayoutStorage() {

    /**
     * Save the layout manifest to persistent storage.
     * @return [Result.success] on successful save, [Result.failure] on error
     */
    suspend fun save(manifest: LayoutManifest): Result<Unit>

    /**
     * Load the layout manifest from persistent storage.
     * @return The loaded manifest, or an empty [LayoutManifest] if none exists yet
     */
    suspend fun load(): Result<LayoutManifest>

    /**
     * Delete the stored layout (reset to empty).
     * @return [Result.success] on successful delete, [Result.failure] on error
     */
    suspend fun delete(): Result<Unit>
}

