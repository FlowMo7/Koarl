package dev.moetz.koarl.backend.manager

import dev.moetz.koarl.api.model.ApiAppData
import dev.moetz.koarl.api.model.ApiDeviceData
import dev.moetz.koarl.api.model.CrashUploadRequestBody
import kotlinx.serialization.Serializable

interface CrashManager {

    @Serializable
    data class CrashGroup(
        val uuid: String,
        val packageName: String,
        val similarities: Similarities,
        val isFatal: Boolean,
        val numberOfCrashes: Long
    ) {
        @Serializable
        data class Similarities(
            val name: String?,
            val message: String?,
            val stackTrace: List<STElement>,
            val cause: Similarities?
        ) {
            @Serializable
            data class STElement(
                val className: String?,
                val methodName: String?,
                val lineNumber: Int?,
                val isNativeMethod: Boolean
            )
        }
    }

    @Serializable
    data class CrashDataItem(
        val appData: ApiAppData,
        val deviceData: ApiDeviceData?,
        val crash: CrashUploadRequestBody.ApiCrash
    )


    suspend fun addCrashes(
        deviceData: ApiDeviceData?,
        appData: ApiAppData,
        crashes: List<CrashUploadRequestBody.ApiCrash>
    )

    suspend fun getCrashGroupsForPackageName(packageName: String): List<CrashGroup>

    suspend fun getCrashesInCrashGroup(
        packageName: String,
        groupId: String
    ): Pair<CrashGroup, List<CrashDataItem>>?

    suspend fun getCrash(packageName: String, crashId: String): CrashDataItem?

}