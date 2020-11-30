package dev.moetz.koarl.backend.manager

import dev.moetz.koarl.api.model.ApiAppData
import dev.moetz.koarl.api.model.ApiDeviceData
import dev.moetz.koarl.api.model.CrashUploadRequestBody
import dev.moetz.koarl.backend.obfuscation.ObfuscationManager
import dev.moetz.koarl.backend.persistence.CrashStorage

class CrashManagerImpl(
    private val crashStorage: CrashStorage,
    private val obfuscationManager: ObfuscationManager
) : CrashManager {

    override suspend fun addCrashes(
        deviceData: ApiDeviceData?,
        appData: ApiAppData,
        crashes: List<CrashUploadRequestBody.ApiCrash>
    ) {
        //TODO as deobfuscation may take some time, move that to some async method and respond the upload call immediately
        val obfuscationMapping = obfuscationManager.getObfuscationMapping(
            packageName = appData.packageName,
            appVersionCode = appData.appVersionCode
        )

        if (obfuscationMapping != null) {
            val deObfuscatedCrashes = crashes.map { crash ->
                crash.copy(
                    throwable = obfuscationManager.deObfuscate(
                        mappingText = obfuscationMapping,
                        throwable = crash.throwable
                    )
                )
            }


            crashStorage.insert(
                deviceData = deviceData,
                appData = appData,
                crashes = deObfuscatedCrashes
            )
        } else {
            crashStorage.insert(
                deviceData = deviceData,
                appData = appData,
                crashes = crashes
            )
        }
    }


    override suspend fun getCrashGroupsForPackageName(packageName: String): List<CrashManager.CrashGroup> {
        return crashStorage.getCrashGroups(packageName)
    }

    override suspend fun getCrashesInCrashGroup(
        packageName: String,
        groupId: String
    ): Pair<CrashManager.CrashGroup, List<CrashManager.CrashDataItem>>? {
        val crashes = crashStorage.getCrashesOfGroup(packageName, groupId)
            .map { crashData ->
                CrashManager.CrashDataItem(
                    appData = crashData.appData,
                    deviceData = crashData.deviceData,
                    crash = crashData.crash
                )
            }
        val group = crashStorage.getCrashGroup(packageName, groupId)
        return if (group != null) {
            group to crashes
        } else {
            null
        }
    }

    override suspend fun getCrash(
        packageName: String,
        crashId: String
    ): CrashManager.CrashDataItem? {
        return crashStorage.getCrash(packageName = packageName, uuid = crashId)
            ?.let { crashData ->
                CrashManager.CrashDataItem(
                    appData = crashData.appData,
                    deviceData = crashData.deviceData,
                    crash = crashData.crash
                )
            }
    }

}