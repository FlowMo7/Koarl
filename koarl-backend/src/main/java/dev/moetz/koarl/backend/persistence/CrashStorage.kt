package dev.moetz.koarl.backend.persistence

import dev.moetz.koarl.api.model.ApiAppData
import dev.moetz.koarl.api.model.ApiDeviceData
import dev.moetz.koarl.api.model.CrashUploadRequestBody
import dev.moetz.koarl.backend.manager.CrashManager
import org.joda.time.DateTime

interface CrashStorage {

    data class CrashData(
        val appData: ApiAppData,
        val deviceData: ApiDeviceData?,
        val crash: CrashUploadRequestBody.ApiCrash
    )

    suspend fun insert(
        deviceData: ApiDeviceData?,
        appData: ApiAppData,
        crashes: List<CrashUploadRequestBody.ApiCrash>
    )

    suspend fun updateThrowable(
        crashUUID: String,
        packageName: String,
        throwable: CrashUploadRequestBody.ApiCrash.ApiThrowable
    )

    suspend fun getCrashes(
        packageName: String? = null,
        versionCode: Long? = null,
        from: DateTime? = null,
        to: DateTime? = null
    ): List<CrashData>

    suspend fun getCrash(
        packageName: String,
        uuid: String
    ): CrashData?


    suspend fun getCrashesOfGroup(
        packageName: String,
        groupId: String
    ): List<CrashData>

    suspend fun getCrashGroups(packageName: String): List<CrashManager.CrashGroup>

    suspend fun getCrashGroup(packageName: String, groupId: String): CrashManager.CrashGroup?

}