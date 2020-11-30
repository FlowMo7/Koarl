package dev.moetz.koarl.android.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import dev.moetz.koarl.api.model.ApiDeviceState

@Entity(tableName = "deviceState")
internal data class LocalDeviceState(
    @PrimaryKey(autoGenerate = true) var primaryKey: Long,
    @ColumnInfo(name = "freeMemory") var freeMemory: Long,
    @ColumnInfo(name = "totalMemory") var totalMemory: Long,
    @ColumnInfo(name = "orientation") var orientationString: String
) {

    @Ignore
    constructor(apiDeviceState: ApiDeviceState) : this(
        primaryKey = 0L,
        freeMemory = apiDeviceState.freeMemory,
        totalMemory = apiDeviceState.totalMemory,
        orientationString = apiDeviceState.orientation.name
    )

    @get:Ignore
    val asApiDeviceState: ApiDeviceState
        get() = ApiDeviceState(
            freeMemory = freeMemory,
            totalMemory = totalMemory,
            orientation = ApiDeviceState.ApiDeviceOrientation.valueOf(orientationString)
        )
}