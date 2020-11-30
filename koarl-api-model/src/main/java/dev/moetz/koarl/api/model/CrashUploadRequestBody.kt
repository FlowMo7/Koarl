package dev.moetz.koarl.api.model

import dev.moetz.koarl.api.serializer.DateTimeSerializer
import kotlinx.serialization.Serializable
import org.joda.time.DateTime

@Serializable
data class CrashUploadRequestBody(
    val deviceData: ApiDeviceData?,
    val appData: ApiAppData,
    val crashes: List<ApiCrash>
) {
    @Serializable
    data class ApiCrash(
        val uuid: String,
        val isFatal: Boolean,
        val inForeground: Boolean,
        @Serializable(with = DateTimeSerializer::class) val dateTime: DateTime,
        val throwable: ApiThrowable,
        val deviceState: ApiDeviceState
    ) {

        @Serializable
        data class ApiThrowable(
            val name: String?,
            val message: String?,
            val localizedMessage: String?,
            val stackTrace: List<ApiStackTraceElement>,
            val cause: ApiThrowable?
        )

        @Serializable
        data class ApiStackTraceElement(
            val fileName: String?,
            val lineNumber: Int?,
            val className: String?,
            val methodName: String?,
            val isNativeMethod: Boolean
        )

    }
}
