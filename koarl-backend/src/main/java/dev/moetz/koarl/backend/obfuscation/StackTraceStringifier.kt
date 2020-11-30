package dev.moetz.koarl.backend.obfuscation

import dev.moetz.koarl.api.model.CrashUploadRequestBody
import dev.moetz.koarl.backend.manager.CrashManager

class StackTraceStringifier {

    fun stackTraceToApiThrowable(
        stackTraceString: String,
        originalApiThrowable: CrashUploadRequestBody.ApiCrash.ApiThrowable,
        cause: CrashUploadRequestBody.ApiCrash.ApiThrowable?
    ): CrashUploadRequestBody.ApiCrash.ApiThrowable {

        val stackTrace = stackTraceString.split('\n')
            .asSequence()
            .drop(1)
            .map { it.trim() }
            .filter { it.startsWith("at ") }
            .mapIndexed { index, line ->
                val correspondingOriginal = originalApiThrowable.stackTrace.getOrNull(index)

                val fileName = line
                    .substringAfterLast("(", "")
                    .substringBefore(")", "")
                    .substringBefore(":", "")
                    .takeIf { it.isNotBlank() }

                val lineNumber = line
                    .substringAfterLast("(", "")
                    .substringBefore(")", "")
                    .substringAfter(":", "")
                    .toIntOrNull()

                val className = line
                    .substringAfter("at ", "")
                    .substringBefore("(", "")
                    .substringBeforeLast(".")
                    .takeIf { it.isNotBlank() }

                val methodName = line
                    .let {
                        if (className != null) {
                            it.substringAfter(className, "")
                        } else {
                            it.substringAfter("at ", "")
                                .substringBefore("(", "")
                                .substringAfter(".")
                                .takeIf { it.isNotBlank() }
                        }
                    }
                    ?.substringBeforeLast("(", "")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { methodName ->
                        //strip return type from method name if present
                        if (methodName.substringBefore("(", "").contains(" ")) {
                            methodName.substringAfter(" ")
                        } else {
                            methodName
                        }
                    }
                    //omit parameter brackets at all
                    ?.substringBefore("(")
                    ?.trim()

                CrashUploadRequestBody.ApiCrash.ApiStackTraceElement(
                    fileName = fileName ?: correspondingOriginal?.fileName,
                    lineNumber = lineNumber ?: correspondingOriginal?.lineNumber,
                    className = className ?: correspondingOriginal?.className,
                    methodName = methodName ?: correspondingOriginal?.methodName,
                    isNativeMethod = correspondingOriginal?.isNativeMethod ?: false
                )
            }
            .toList()

        return CrashUploadRequestBody.ApiCrash.ApiThrowable(
            name = originalApiThrowable.name,
            localizedMessage = originalApiThrowable.localizedMessage,
            message = originalApiThrowable.message,
            stackTrace = stackTrace,
            cause = cause
        )
    }

    fun getStackTraceString(similarities: CrashManager.CrashGroup.Similarities): String {
        val stringBuilder = StringBuilder()

        stringBuilder.append("${similarities.name}: ${similarities.message}").append('\n')

        similarities.stackTrace.forEach { stackTraceElement ->
            stringBuilder.append("\tat ${stackTraceElement.stackTraceLine}").append('\n')
        }

        var cause = similarities.cause
        while (cause != null) {
            stringBuilder.append("Caused by: ")
            stringBuilder.append("${cause.name}: ${cause.message}").append('\n')
            cause.stackTrace.forEach { stackTraceElement ->
                stringBuilder.append("\tat ${stackTraceElement.stackTraceLine}").append('\n')
            }

            cause = cause.cause
        }

        return stringBuilder.toString()

    }


    fun getStackTraceString(
        apiThrowable: CrashUploadRequestBody.ApiCrash.ApiThrowable,
        includeCause: Boolean = true
    ): String {
        val stringBuilder = StringBuilder()

        stringBuilder.append(apiThrowable.stackTraceFirstLine).append('\n')

        apiThrowable.stackTrace.forEach { stackTraceElement ->
            stringBuilder.append("\tat ${stackTraceElement.stackTraceLine}").append('\n')
        }

        if (includeCause) {
            apiThrowable.cause?.let { cause ->
                printEnclosedStackTrace(cause, stringBuilder, "Caused by: ", "")
            }
        }

        return stringBuilder.toString()
    }


    private fun printEnclosedStackTrace(
        apiThrowable: CrashUploadRequestBody.ApiCrash.ApiThrowable,
        stringBuilder: StringBuilder,
        prefix: String,
        indentation: String
    ) {
        stringBuilder.append(indentation + prefix + apiThrowable.stackTraceFirstLine).append('\n')

        apiThrowable.stackTrace.forEach { stackTraceElement ->
            stringBuilder.append(indentation + "\tat " + stackTraceElement.stackTraceLine)
                .append('\n')
        }

        apiThrowable.cause?.let { cause ->
            printEnclosedStackTrace(cause, stringBuilder, "Caused by: ", indentation)
        }

    }


    private val CrashUploadRequestBody.ApiCrash.ApiThrowable.stackTraceFirstLine: String
        get() {
            val var1 = this.javaClass.name
            val var2 = this.localizedMessage ?: message
            return if (var2 != null) "$var1: $var2" else var1
        }

    private val CrashUploadRequestBody.ApiCrash.ApiStackTraceElement.stackTraceLine: String
        get() = this.className + "." + this.methodName + if (this.isNativeMethod) "(Native Method)" else if (this.fileName != null && this.lineNumber ?: 0 >= 0) "(" + this.fileName + ":" + this.lineNumber + ")" else if (this.fileName != null) "(" + this.fileName + ")" else "(Unknown Source)"


    private val CrashManager.CrashGroup.Similarities.STElement.stackTraceLine: String
        get() = this.className + "." + this.methodName + if (this.isNativeMethod) "(Native Method)" else if (this.lineNumber ?: 0 >= 0) "(" + this.lineNumber + ")" else "(Unknown Source)"


}