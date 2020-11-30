package dev.moetz.koarl.android.data.remote

import dev.moetz.koarl.api.model.ApiAppData
import dev.moetz.koarl.api.model.ApiDeviceData
import dev.moetz.koarl.api.model.CrashUploadRequestBody

interface CrashUploader {

    /**
     * @param baseUrl The configured base-url for the KOARL backend service.
     * @param deviceData static data about the device. Will be null if the privacySettings have
     * disabled sharing this data.
     * @param appData static data about the app (app-name, package name and app version)
     *
     *
     * @return whether the operation was successful
     */
    suspend fun uploadCrashes(
        baseUrl: String,
        deviceData: ApiDeviceData?,
        appData: ApiAppData,
        crashes: List<CrashUploadRequestBody.ApiCrash>
    ): Boolean

}