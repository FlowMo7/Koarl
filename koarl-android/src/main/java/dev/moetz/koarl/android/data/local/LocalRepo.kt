package dev.moetz.koarl.android.data.local

import dev.moetz.koarl.api.model.CrashUploadRequestBody


/**
 * Interface for the local caching of crashes (fatal and non-fatal) and heartbeats / session-data.
 *
 * There is a default implementation using Room available, if you want/need your own implementation
 * (e.g. for security concerns), you need to implement this interface and register it while
 * initializing the library.
 */
interface LocalRepo {

    /**
     * Adds a crash to the local storage.
     *
     * @param crash the Crash to store
     */
    suspend fun addCrash(crash: CrashUploadRequestBody.ApiCrash)

    /**
     * Get all currently stored crashes.
     *
     * @return A list of all crashes currently stored.
     */
    suspend fun getCrashes(): List<CrashUploadRequestBody.ApiCrash>

    /**
     * Removes the crashes with the given [ids] from the local storage.
     *
     * @param ids The UUIDs of the crashes to remove from the local storage.
     */
    suspend fun removeCrashes(ids: List<String>)

}