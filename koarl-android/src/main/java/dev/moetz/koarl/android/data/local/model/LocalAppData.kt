package dev.moetz.koarl.android.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import dev.moetz.koarl.api.model.ApiAppData

@Entity(tableName = "deviceData")
internal data class LocalAppData(
    @PrimaryKey(autoGenerate = true) var primaryKey: Long,
    @ColumnInfo(name = "packageName") var packageName: String,
    @ColumnInfo(name = "appName") var appName: String,
    @ColumnInfo(name = "appVersionCode") var appVersionCode: Long,
    @ColumnInfo(name = "appVersionName") var appVersionName: String
) {

    @Ignore
    constructor(apiDeviceState: ApiAppData) : this(
        primaryKey = 0L,
        packageName = apiDeviceState.packageName,
        appName = apiDeviceState.appName,
        appVersionCode = apiDeviceState.appVersionCode,
        appVersionName = apiDeviceState.appVersionName
    )

    @get:Ignore
    val asApiAppData: ApiAppData
        get() = ApiAppData(
            packageName = packageName,
            appName = appName,
            appVersionCode = appVersionCode,
            appVersionName = appVersionName
        )
}