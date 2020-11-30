package dev.moetz.koarl.backend.persistence.sql

import dev.moetz.koarl.api.model.ApiAppData
import dev.moetz.koarl.api.model.ApiDeviceData
import dev.moetz.koarl.api.model.ApiDeviceState
import dev.moetz.koarl.api.model.CrashUploadRequestBody
import dev.moetz.koarl.backend.manager.CrashManager
import dev.moetz.koarl.backend.persistence.CrashStorage
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime
import java.util.*

class SqlCrashStorage(
    private val json: Json,
    private val database: Database
) : CrashStorage {


    override suspend fun insert(
        deviceData: ApiDeviceData?,
        appData: ApiAppData,
        crashes: List<CrashUploadRequestBody.ApiCrash>
    ) {
        suspendedTransaction(database) {
            val deviceDataId = if (deviceData != null) {
                val existingDeviceDataId = DeviceDataTable
                    .slice(DeviceDataTable.id)
                    .select {
                        (DeviceDataTable.deviceName eq deviceData.deviceName) and
                                (DeviceDataTable.manufacturer eq deviceData.manufacturer) and
                                (DeviceDataTable.brand eq deviceData.brand) and
                                (DeviceDataTable.model eq deviceData.model) and
                                (DeviceDataTable.buildId eq deviceData.buildId) and
                                (DeviceDataTable.operationSystemVersion eq deviceData.operationSystemVersion)
                    }
                    .singleOrNull()
                    ?.get(DeviceDataTable.id)

                if (existingDeviceDataId == null) {
                    DeviceDataTable.insert {
                        it[DeviceDataTable.deviceName] = deviceData.deviceName
                        it[DeviceDataTable.manufacturer] = deviceData.manufacturer
                        it[DeviceDataTable.brand] = deviceData.brand
                        it[DeviceDataTable.model] = deviceData.model
                        it[DeviceDataTable.buildId] = deviceData.buildId
                        it[DeviceDataTable.operationSystemVersion] =
                            deviceData.operationSystemVersion
                    }.get(DeviceDataTable.id)
                } else {
                    existingDeviceDataId
                }
            } else {
                null
            }

            val existingAppDataId = AppDataTable
                .slice(AppDataTable.id)
                .select {
                    (AppDataTable.packageName eq appData.packageName) and
                            (AppDataTable.appName eq appData.appName) and
                            (AppDataTable.appVersionCode eq appData.appVersionCode) and
                            (AppDataTable.appVersionName eq appData.appVersionName)
                }
                .singleOrNull()
                ?.get(AppDataTable.id)
            val appDataId = if (existingAppDataId == null) {
                AppDataTable.insert {
                    it[AppDataTable.packageName] = appData.packageName
                    it[AppDataTable.appName] = appData.appName
                    it[AppDataTable.appVersionCode] = appData.appVersionCode
                    it[AppDataTable.appVersionName] = appData.appVersionName
                }.get(AppDataTable.id)
            } else {
                existingAppDataId
            }

            crashes.forEach { crash ->
                try {
                    val serializedThrowable = crash.throwable.toStoredJson()

                    val groupUuid = (CrashTable innerJoin AppDataTable)
                        .slice(CrashTable.groupUuid)
                        .select {
                            //TODO ugly workaround as quals statement seems to have been too long.
                            (AppDataTable.packageName eq appData.packageName) and
                                    (CrashTable.throwable like "${serializedThrowable.substring(0..128)}%")
                        }
                        .limit(1)
                        .firstOrNull()
                        ?.let { resultRow -> resultRow[CrashTable.groupUuid] }
                        ?: UUID.randomUUID()

                    CrashTable.insert {
                        it[CrashTable.uuid] = UUID.fromString(crash.uuid)
                        it[CrashTable.isFatal] = crash.isFatal
                        it[CrashTable.inForeground] = crash.inForeground
                        it[CrashTable.dateTime] = crash.dateTime
                        it[CrashTable.throwable] = serializedThrowable
                        it[CrashTable.deviceData] = deviceDataId
                        it[CrashTable.appData] = appDataId
                        it[CrashTable.groupUuid] = groupUuid

                        //device state:
                        it[CrashTable.freeMemory] = crash.deviceState.freeMemory
                        it[CrashTable.totalMemory] = crash.deviceState.totalMemory
                        it[CrashTable.orientation] = when (crash.deviceState.orientation) {
                            ApiDeviceState.ApiDeviceOrientation.Portrait -> CrashTable.DeviceOrientation.Portrait
                            ApiDeviceState.ApiDeviceOrientation.Landscape -> CrashTable.DeviceOrientation.Landscape
                            ApiDeviceState.ApiDeviceOrientation.Undefined -> CrashTable.DeviceOrientation.Undefined
                        }
                    }
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                    throw throwable
                }
            }
        }
    }


    override suspend fun updateThrowable(
        crashUUID: String,
        packageName: String,
        throwable: CrashUploadRequestBody.ApiCrash.ApiThrowable
    ) {
        return suspendedTransaction(database) {
            CrashTable.update(
                where = {
                    (CrashTable.uuid eq UUID.fromString(crashUUID)) and
                            (CrashTable.appData inSubQuery AppDataTable.select { AppDataTable.appName eq packageName })
                },
                body = {
                    it[CrashTable.throwable] = throwable.toStoredJson()
                }
            )
        }
    }


    private fun ResultRow.toCrashData(): CrashStorage.CrashData {
        return CrashStorage.CrashData(
            crash = CrashUploadRequestBody.ApiCrash(
                uuid = this[CrashTable.uuid].toString(),
                isFatal = this[CrashTable.isFatal],
                inForeground = this[CrashTable.inForeground],
                dateTime = this[CrashTable.dateTime],
                throwable = this[CrashTable.throwable].fromStoredJson(),
                deviceState = ApiDeviceState(
                    freeMemory = this[CrashTable.freeMemory],
                    totalMemory = this[CrashTable.totalMemory],
                    orientation = this[CrashTable.orientation].let {
                        when (it) {
                            CrashTable.DeviceOrientation.Portrait -> ApiDeviceState.ApiDeviceOrientation.Portrait
                            CrashTable.DeviceOrientation.Landscape -> ApiDeviceState.ApiDeviceOrientation.Landscape
                            CrashTable.DeviceOrientation.Undefined -> ApiDeviceState.ApiDeviceOrientation.Undefined
                        }
                    }
                )
            ),
            appData = ApiAppData(
                packageName = this[AppDataTable.packageName],
                appName = this[AppDataTable.appName],
                appVersionCode = this[AppDataTable.appVersionCode],
                appVersionName = this[AppDataTable.appVersionName]
            ),
            deviceData = ApiDeviceData(
                deviceName = this[DeviceDataTable.deviceName],
                manufacturer = this[DeviceDataTable.manufacturer],
                brand = this[DeviceDataTable.brand],
                model = this[DeviceDataTable.model],
                buildId = this[DeviceDataTable.buildId],
                operationSystemVersion = this[DeviceDataTable.operationSystemVersion]
            )
        )
    }

    override suspend fun getCrashes(
        packageName: String?,
        versionCode: Long?,
        from: DateTime?,
        to: DateTime?
    ): List<CrashStorage.CrashData> {
        return suspendedTransaction(database) {
            (CrashTable innerJoin AppDataTable innerJoin DeviceDataTable)
                .selectAll()
                .map { resultRow -> resultRow.toCrashData() }
        }
    }

    override suspend fun getCrash(packageName: String, uuid: String): CrashStorage.CrashData? {
        return suspendedTransaction(database) {
            (CrashTable innerJoin AppDataTable innerJoin DeviceDataTable)
                .select {
                    (CrashTable.uuid eq UUID.fromString(uuid)) and
                            (AppDataTable.packageName eq packageName)
                }
                .singleOrNull()
                ?.toCrashData()
        }
    }

    override suspend fun getCrashesOfGroup(
        packageName: String,
        groupId: String
    ): List<CrashStorage.CrashData> {
        return suspendedTransaction(database) {
            (CrashTable innerJoin AppDataTable innerJoin DeviceDataTable)
                .select {
                    (AppDataTable.packageName eq packageName) and (CrashTable.groupUuid eq UUID.fromString(
                        groupId
                    ))
                }
                .asSequence()
                .map { resultRow -> resultRow.toCrashData() }
                .toList()
        }
    }


    override suspend fun getCrashGroups(packageName: String): List<CrashManager.CrashGroup> {
        return suspendedTransaction(database) {
            (CrashTable innerJoin AppDataTable)
                .slice(
                    CrashTable.groupUuid,
                    AppDataTable.packageName,
                    CrashTable.throwable,
                    CrashTable.isFatal,
                    CrashTable.groupUuid.count()
                )
                .select { AppDataTable.packageName eq packageName }
                .groupBy(CrashTable.groupUuid, CrashTable.throwable, CrashTable.isFatal)
                .map { resultRow -> resultRow.toCrashGroup() }
        }
    }

    private fun ResultRow.toCrashGroup(): CrashManager.CrashGroup {
        return CrashManager.CrashGroup(
            uuid = this[CrashTable.groupUuid].toString(),
            packageName = this[AppDataTable.packageName],
            similarities = this[CrashTable.throwable].fromStoredJson()
                .toSimilarities(),
            isFatal = this[CrashTable.isFatal],
            numberOfCrashes = this[CrashTable.groupUuid.count()]
        )
    }

    override suspend fun getCrashGroup(
        packageName: String,
        groupId: String
    ): CrashManager.CrashGroup? {
        return suspendedTransaction(database) {
            (CrashTable innerJoin AppDataTable)
                .slice(
                    CrashTable.groupUuid,
                    AppDataTable.packageName,
                    CrashTable.throwable,
                    CrashTable.isFatal,
                    CrashTable.groupUuid.count()
                )
                .select {
                    (AppDataTable.packageName eq packageName) and
                            (CrashTable.groupUuid eq UUID.fromString(groupId))
                }
                .groupBy(CrashTable.groupUuid, CrashTable.throwable, CrashTable.isFatal)
                .limit(1)
                .firstOrNull()
                ?.toCrashGroup()
        }
    }


    private fun CrashUploadRequestBody.ApiCrash.ApiThrowable.toStoredJson(): String {
        return json.stringify(
            CrashUploadRequestBody.ApiCrash.ApiThrowable.serializer(),
            this
        )
    }

    private fun String.fromStoredJson(): CrashUploadRequestBody.ApiCrash.ApiThrowable {
        return json.parse(
            CrashUploadRequestBody.ApiCrash.ApiThrowable.serializer(),
            this
        )
    }

    private fun CrashUploadRequestBody.ApiCrash.ApiThrowable.toSimilarities(): CrashManager.CrashGroup.Similarities {
        return CrashManager.CrashGroup.Similarities(
            name = this.name,
            message = this.message,
            stackTrace = this.stackTrace.map {
                CrashManager.CrashGroup.Similarities.STElement(
                    className = it.className,
                    methodName = it.methodName,
                    lineNumber = it.lineNumber,
                    isNativeMethod = it.isNativeMethod
                )
            },
            cause = this.cause?.toSimilarities()
        )
    }

}