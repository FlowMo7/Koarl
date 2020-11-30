package dev.moetz.koarl.backend.dashboard.api

import dev.moetz.koarl.api.model.ApiAppData
import dev.moetz.koarl.api.model.ApiDeviceData
import dev.moetz.koarl.api.model.CrashUploadRequestBody
import dev.moetz.koarl.backend.dashboard.MaterialDashboard
import dev.moetz.koarl.backend.manager.CrashManager
import dev.moetz.koarl.backend.obfuscation.ObfuscationManager
import dev.moetz.koarl.backend.persistence.AppStorage
import dev.moetz.koarl.backend.persistence.CrashStorage
import kotlinx.serialization.Serializable

class DashboardApi(
    private val appStorage: AppStorage,
    private val crashStorage: CrashStorage,
    private val obfuscationManager: ObfuscationManager
) {

    enum class CrashType(val key: String) {
        All("all"),
        OnlyFatal("fatal"),
        OnlyNonFatal("nonfatal")
    }

    @Serializable
    data class CrashesForAppsResponse(
        val crashes: List<CrashGroup>
    )

    @Serializable
    data class CrashInGroupResponse(
        val group: CrashGroup,
        val crash: Crash
    )


    @Serializable
    data class CrashListInGroupResponse(
        val group: CrashGroup,
        val crashes: List<Crash>
    )


    @Serializable
    data class Crash(
        val appData: ApiAppData,
        val deviceData: ApiDeviceData?,
        val crash: CrashUploadRequestBody.ApiCrash
    )

    @Serializable
    data class CrashGroup(
        val groupId: String,
        val throwable: SneakPeakThrowable,
        val numberOfCrashes: Long
    ) {
        @Serializable
        data class SneakPeakThrowable(
            val name: String?,
            val message: String?,
            val stackTrace: List<STElement>,
            val cause: SneakPeakThrowable?
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
    data class AppsResponse(
        val apps: List<App>
    ) {
        @Serializable
        data class App(
            val packageName: String,
            val appName: String
        )
    }

    @Serializable
    data class ObfuscationMappingVersionCodesResponse(
        val versionCodes: List<Long>
    )


    suspend fun getAllApps(): AppsResponse {
        return AppsResponse(
            apps = appStorage.getStoredApps().map {
                AppsResponse.App(
                    packageName = it.packageName,
                    appName = it.appName
                )
            }
        )
    }


    suspend fun getCrashGroupsForApp(
        packageName: String,
        crashType: CrashType
    ): CrashesForAppsResponse? {
        return crashStorage.getCrashGroups(packageName)
            .let { all ->
                when (crashType) {
                    CrashType.All -> all
                    CrashType.OnlyFatal -> all.filter { it.isFatal }
                    CrashType.OnlyNonFatal -> all.filterNot { it.isFatal }
                }
            }
            .let { storedCrashGroups ->
                CrashesForAppsResponse(
                    crashes = storedCrashGroups.map { crashGroup ->
                        crashGroup.toApiResponseItem()
                    }
                )
            }
    }


    suspend fun getCrashInGroup(
        packageName: String,
        groupId: String,
        crashId: String
    ): CrashInGroupResponse? {
        val crash = crashStorage.getCrash(
            packageName = packageName,
            uuid = crashId
        )

        val group = crashStorage.getCrashGroup(packageName = packageName, groupId = groupId)

        return if (group != null && crash != null) {
            CrashInGroupResponse(
                group = group.toApiResponseItem(),
                crash = Crash(
                    appData = crash.appData,
                    deviceData = crash.deviceData,
                    crash = crash.crash
                )
            )
        } else {
            null
        }
    }

    suspend fun getGroupCrashes(
        packageName: String,
        groupId: String
    ): CrashListInGroupResponse? {
        val crashes = crashStorage.getCrashesOfGroup(
            packageName = packageName,
            groupId = groupId
        )

        val group = crashStorage.getCrashGroup(packageName = packageName, groupId = groupId)

        return if (group != null) {
            CrashListInGroupResponse(
                group = group.toApiResponseItem(),
                crashes = crashes.map { crashData ->
                    Crash(
                        appData = crashData.appData,
                        deviceData = crashData.deviceData,
                        crash = crashData.crash
                    )
                }
            )
        } else {
            null
        }
    }


    suspend fun addObfuscationMapping(
        packageName: String,
        appVersionCode: Long,
        mappingFileContents: String
    ) {
        obfuscationManager.addMappingFile(
            packageName = packageName,
            appVersionCode = appVersionCode,
            mappingFileContents = mappingFileContents
        )
    }

    suspend fun getObfuscationMapping(packageName: String, appVersionCode: Long): String? {
        return obfuscationManager.getObfuscationMapping(
            packageName = packageName,
            appVersionCode = appVersionCode
        )
    }

    suspend fun getObfuscationMappingVersionCodes(packageName: String): ObfuscationMappingVersionCodesResponse {
        return ObfuscationMappingVersionCodesResponse(
            obfuscationManager.getObfuscationMappingVersionCodes(packageName = packageName)
        )
    }


    private fun CrashManager.CrashGroup.toApiResponseItem(): CrashGroup {
        return CrashGroup(
            groupId = uuid,
            throwable = similarities.toSneakPeakThrowable(),
            numberOfCrashes = numberOfCrashes
        )
    }

    private fun CrashManager.CrashGroup.Similarities.toSneakPeakThrowable(): CrashGroup.SneakPeakThrowable {
        return CrashGroup.SneakPeakThrowable(
            name = name,
            message = message,
            stackTrace = stackTrace.map {
                CrashGroup.SneakPeakThrowable.STElement(
                    className = it.className,
                    methodName = it.methodName,
                    lineNumber = it.lineNumber,
                    isNativeMethod = it.isNativeMethod
                )
            },
            cause = cause?.toSneakPeakThrowable()
        )
    }


}