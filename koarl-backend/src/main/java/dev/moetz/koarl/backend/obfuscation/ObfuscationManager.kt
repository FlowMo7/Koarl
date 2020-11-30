package dev.moetz.koarl.backend.obfuscation

import dev.moetz.koarl.api.model.CrashUploadRequestBody
import dev.moetz.koarl.backend.persistence.CrashStorage
import dev.moetz.koarl.backend.persistence.ObfuscationMappingStorage
import proguard.retrace.ReTrace
import java.io.File
import java.io.LineNumberReader
import java.io.PrintWriter
import java.io.StringWriter

class ObfuscationManager(
    private val obfuscationMappingStorage: ObfuscationMappingStorage,
    private val stackTraceStringifier: StackTraceStringifier,
    private val crashStorage: CrashStorage,
    private val tempFileDirectory: File = File("/tmp/obfuscation")
) {

    private fun createMappingFile(mappingText: String): File {
        tempFileDirectory.mkdirs()
        return File.createTempFile("mapping", ".tmp", tempFileDirectory).apply {
            writeText(mappingText)
        }
    }


    suspend fun addMappingFile(
        packageName: String,
        appVersionCode: Long,
        mappingFileContents: String
    ) {
        obfuscationMappingStorage.insertMapping(
            packageName = packageName,
            appVersionCode = appVersionCode,
            mappingFileContents = mappingFileContents
        )
        //TODO apply deobfuscation automatically to all crashes present in the crash store yet?


        val crashes = crashStorage.getCrashes(
            packageName = packageName,
            versionCode = appVersionCode
        )

        crashes.forEach { crashData ->
            crashStorage.updateThrowable(
                crashUUID = crashData.crash.uuid,
                packageName = packageName,
                throwable = deObfuscate(
                    mappingText = mappingFileContents,
                    throwable = crashData.crash.throwable
                )
            )
        }


        //TODO regenerate groups!
    }

    suspend fun getObfuscationMapping(packageName: String, appVersionCode: Long): String? {
        return obfuscationMappingStorage.getMapping(
            packageName = packageName,
            appVersionCode = appVersionCode
        )
    }

    suspend fun getObfuscationMappingVersionCodes(packageName: String): List<Long> {
        return obfuscationMappingStorage.getStoredMappingsForPackageName(packageName = packageName)
    }


    suspend fun deObfuscate(
        packageName: String,
        appVersionCode: Long,
        throwables: Iterable<CrashUploadRequestBody.ApiCrash.ApiThrowable>
    ): Iterable<CrashUploadRequestBody.ApiCrash.ApiThrowable> {
        val mappingText = getObfuscationMapping(
            packageName = packageName,
            appVersionCode = appVersionCode
        )
        return if (mappingText != null) {
            val mappingFile = createMappingFile(mappingText)

            val reTrace = ReTrace(
                ReTrace.STACK_TRACE_EXPRESSION,
                false,
                mappingFile
            )

            try {
                throwables.map { throwable ->
                    deobFullStackTraceWithCause(
                        reTrace = reTrace,
                        obfuscated = throwable
                    )
                }
            } finally {
                mappingFile.delete()
            }
        } else {
            throw NoSuchElementException("No obfuscation mapping for packageName '$packageName' and appVersionCode '$appVersionCode' found.")
        }
    }


    fun deObfuscate(
        mappingText: String,
        throwable: CrashUploadRequestBody.ApiCrash.ApiThrowable
    ): CrashUploadRequestBody.ApiCrash.ApiThrowable {
        val mappingFile = createMappingFile(mappingText)

        val reTrace = ReTrace(
            ReTrace.STACK_TRACE_EXPRESSION,
            false,
            mappingFile
        )

        return try {
            deobFullStackTraceWithCause(
                reTrace = reTrace,
                obfuscated = throwable
            )
        } finally {
            mappingFile.delete()
        }
    }


    private fun deobFullStackTraceWithCause(
        reTrace: ReTrace,
        obfuscated: CrashUploadRequestBody.ApiCrash.ApiThrowable
    ): CrashUploadRequestBody.ApiCrash.ApiThrowable {
        var subject: CrashUploadRequestBody.ApiCrash.ApiThrowable? = obfuscated

        val stack = mutableListOf<CrashUploadRequestBody.ApiCrash.ApiThrowable>()

        while (subject != null) {
            stack.add(deobSingleStackTraceWithoutCause(reTrace, subject))
            subject = subject.cause
        }

        val deobfuscated = stack.reversed()
            .fold<CrashUploadRequestBody.ApiCrash.ApiThrowable, CrashUploadRequestBody.ApiCrash.ApiThrowable?>(
                null
            ) { acc, item -> item.copy(cause = acc) }!!
        return deobfuscated
    }

    private fun deobSingleStackTraceWithoutCause(
        reTrace: ReTrace,
        obfuscated: CrashUploadRequestBody.ApiCrash.ApiThrowable
    ): CrashUploadRequestBody.ApiCrash.ApiThrowable {
        val stringWriter = StringWriter()

        reTrace.retrace(
            LineNumberReader(
                stackTraceStringifier.getStackTraceString(
                    apiThrowable = obfuscated,
                    includeCause = false
                ).reader()
            ),
            PrintWriter(stringWriter)
        )

        val deobString = stringWriter.toString()

        val deobfuscatedApiThrowable = stackTraceStringifier.stackTraceToApiThrowable(
            stackTraceString = deobString,
            originalApiThrowable = obfuscated,
            cause = null
        )

        return deobfuscatedApiThrowable
    }

}