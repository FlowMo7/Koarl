package dev.moetz.koarl.android.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import dev.moetz.koarl.api.model.CrashUploadRequestBody

@Entity(tableName = "stackTrace")
internal data class LocalStackTraceElement(
    @PrimaryKey(autoGenerate = true) var primaryKey: Long,
    @ColumnInfo(name = "throwableId") var throwableId: Long,
    @ColumnInfo(name = "stackTracePosition") var stackTracePosition: Int,
    @ColumnInfo(name = "fileName") var fileName: String?,
    @ColumnInfo(name = "lineNumber") var lineNumber: Int?,
    @ColumnInfo(name = "className") var className: String?,
    @ColumnInfo(name = "methodName") var methodName: String?,
    @ColumnInfo(name = "nativeMethod") var isNativeMethod: Boolean
) {

    @Ignore
    constructor(
        throwableId: Long,
        stackTracePosition: Int,
        apiStackTraceElement: CrashUploadRequestBody.ApiCrash.ApiStackTraceElement
    ) : this(
        primaryKey = 0L,
        throwableId = throwableId,
        stackTracePosition = stackTracePosition,
        fileName = apiStackTraceElement.fileName,
        lineNumber = apiStackTraceElement.lineNumber,
        className = apiStackTraceElement.className,
        methodName = apiStackTraceElement.methodName,
        isNativeMethod = apiStackTraceElement.isNativeMethod
    )

    @get:Ignore
    val asApiStackTraceElement: CrashUploadRequestBody.ApiCrash.ApiStackTraceElement
        get() = CrashUploadRequestBody.ApiCrash.ApiStackTraceElement(
            fileName = fileName,
            lineNumber = lineNumber,
            className = className,
            methodName = methodName,
            isNativeMethod = isNativeMethod
        )

}