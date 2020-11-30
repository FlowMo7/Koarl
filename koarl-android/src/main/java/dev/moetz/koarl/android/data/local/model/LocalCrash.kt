package dev.moetz.koarl.android.data.local.model

import androidx.room.*
import dev.moetz.koarl.api.model.CrashUploadRequestBody
import dev.moetz.koarl.api.serializer.DateTimeSerializer

@Entity(tableName = "crash")
@SuppressWarnings(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
internal data class LocalCrash(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "uuid") var uuid: String,
    @ColumnInfo(name = "fatal") var isFatal: Boolean,
    @ColumnInfo(name = "foreground") var inForeground: Boolean,
    @ColumnInfo(name = "dateTime") var dateTime: String,
    @Embedded(prefix = "deviceState_") var deviceState: LocalDeviceState
) {
    @Ignore
    constructor(
        apiCrash: CrashUploadRequestBody.ApiCrash
    ) : this(
        uuid = apiCrash.uuid,
        isFatal = apiCrash.isFatal,
        inForeground = apiCrash.inForeground,
        dateTime = DateTimeSerializer.serializeToString(apiCrash.dateTime),
        deviceState = LocalDeviceState(apiCrash.deviceState)
    )
}