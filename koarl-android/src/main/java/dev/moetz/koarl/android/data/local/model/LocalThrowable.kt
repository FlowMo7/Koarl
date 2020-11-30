package dev.moetz.koarl.android.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import dev.moetz.koarl.api.model.CrashUploadRequestBody

/**
 * @param primaryKey The primary key for the [LocalThrowable].
 * @param crashUUID The [LocalCrash.uuid] this [LocalThrowable] is referenced to.
 * There is a great chance that multiple [LocalThrowable] are referencing the came [LocalCrash], as
 * each cause of a [LocalThrowable] is stored as a separate [LocalThrowable] here (see [causeDepth]).
 * @param causeDepth the _depth_ of this [LocalThrowable]. As the [LocalThrowable] to a [LocalCrash]
 * and its causes are stored side-by-side here in this table, the [causeDepth] indicates which cause
 * it was.
 * So the [LocalThrowable] with [causeDepth]=0 is the actual [LocalThrowable] of the [LocalCrash],
 * the [LocalThrowable] with [causeDepth]=1 is the cause of the actual [LocalThrowable]
 * (the one with [causeDepth]=0) and so on.
 * @param name The name of the class of the [Throwable].
 * @param message The message of the [Throwable].
 * @param localizedMessage The localized message of the [Throwable].
 */
@Entity(tableName = "throwable")
internal data class LocalThrowable(
    @PrimaryKey(autoGenerate = true) var primaryKey: Long,
    @ColumnInfo(name = "crash") var crashUUID: String,
    @ColumnInfo(name = "depth") var causeDepth: Int,
    @ColumnInfo(name = "name") var name: String?,
    @ColumnInfo(name = "message") var message: String?,
    @ColumnInfo(name = "localizedMessage") var localizedMessage: String?
) {

    @Ignore
    constructor(
        crashUUID: String,
        causeDepth: Int,
        apiThrowable: CrashUploadRequestBody.ApiCrash.ApiThrowable
    ) : this(
        primaryKey = 0L,    //primaryKey set to 0L to get one generated when inserting
        crashUUID = crashUUID,
        causeDepth = causeDepth,
        name = apiThrowable.name,
        message = apiThrowable.message,
        localizedMessage = apiThrowable.localizedMessage
    )

    fun toApiThrowable(
        stackTraceElements: List<CrashUploadRequestBody.ApiCrash.ApiStackTraceElement>,
        cause: CrashUploadRequestBody.ApiCrash.ApiThrowable?
    ): CrashUploadRequestBody.ApiCrash.ApiThrowable {
        return CrashUploadRequestBody.ApiCrash.ApiThrowable(
            name = name,
            message = message,
            localizedMessage = localizedMessage,
            stackTrace = stackTraceElements,
            cause = cause
        )
    }
}