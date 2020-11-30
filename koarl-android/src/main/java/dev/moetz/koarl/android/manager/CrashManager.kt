package dev.moetz.koarl.android.manager

import dev.moetz.koarl.android.KoarlLogger
import dev.moetz.koarl.android.dsl.KoarlConfiguration
import dev.moetz.koarl.android.launchInProcessLifecycleScope
import dev.moetz.koarl.android.manager.session.LifecycleManager
import dev.moetz.koarl.api.model.CrashUploadRequestBody
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import java.util.*

internal class CrashManager(
    private val lifecycleManager: LifecycleManager,
    private val configurationProvider: () -> KoarlConfiguration
) {

    private val configuration: KoarlConfiguration get() = configurationProvider.invoke()

    private val uploadLogicManager = UploadLogicManager()

    fun addCrash(isFatal: Boolean, throwable: Throwable) {
        KoarlLogger.log { "CrashManager.addCrash(isFatal: $isFatal, throwable: $throwable)" }
        val config = configuration
        if(config.privacySettings.enableReporting) {
            runBlocking {
                // this needs to run blocking, as this ie either called from a non-fatal crash
                // (were it should not matter), or from the thread-uncaught-exception-handler, where we
                // need to be blocking as the application is shutting down.

                config.localRepo.addCrash(
                    CrashUploadRequestBody.ApiCrash(
                        uuid = UUID.randomUUID().toString(),
                        isFatal = isFatal,
                        inForeground = lifecycleManager.isInForeground,
                        dateTime = DateTime.now(),
                        throwable = throwable.toApiThrowable(),
                        deviceState = config.deviceStateRetriever.getDeviceState()
                    )
                )
                if (isFatal.not()) {
                    launchInProcessLifecycleScope {
                        triggerCrashUpload()
                    }
                }
            }
        } else {
            KoarlLogger.log { "enableReporting is disabled. Not going to save this crash." }
        }
    }

    private fun Throwable.toApiThrowable(): CrashUploadRequestBody.ApiCrash.ApiThrowable {
        return CrashUploadRequestBody.ApiCrash.ApiThrowable(
            name = this::class.java.simpleName,
            message = this.message,
            localizedMessage = this.localizedMessage,
            stackTrace = this.stackTrace.toApiStackTraceList(),
            cause = this.cause?.toApiThrowable()
        )
    }

    private fun Array<StackTraceElement>.toApiStackTraceList(): List<CrashUploadRequestBody.ApiCrash.ApiStackTraceElement> {
        return this.map {
            CrashUploadRequestBody.ApiCrash.ApiStackTraceElement(
                fileName = it.fileName,
                lineNumber = it.lineNumber,
                className = it.className,
                methodName = it.methodName,
                isNativeMethod = it.isNativeMethod
            )
        }
    }


    fun init() {
        if(configuration.privacySettings.enableReporting) {
            launchInProcessLifecycleScope {
                // Delay initial upload of crashes for 5 seconds, in order to prevent application
                // startup performance decrease
                delay(configuration.timingSettings.delayAfterApplicationStartToUpload)

                triggerCrashUpload()
            }
        }
    }

    private suspend fun triggerCrashUpload() {
        KoarlLogger.log { "CrashManager.triggerCrashUpload()" }

        uploadLogicManager.uploadAndRetryLogic(configuration.timingSettings.delayToRetryUpload) {
            val (crashes, hasMore) = configuration.localRepo.getCrashes().let { allCrashes ->
                if (allCrashes.size > NUMBER_OF_BATCH_SEND_ITEMS) {
                    allCrashes.take(NUMBER_OF_BATCH_SEND_ITEMS) to true
                } else {
                    allCrashes to false
                }
            }

            KoarlLogger.log { "CrashManager.uploadCrashes(), contains ${crashes.size} crashes" }

            if (crashes.isNotEmpty()) {
                val success = configuration.crashUploader.uploadCrashes(
                    baseUrl = configuration.baseUrl,
                    deviceData = configuration.deviceStateRetriever.getDeviceData(),
                    appData = configuration.appData,
                    crashes = crashes
                )

                if (success) {
                    KoarlLogger.log { "CrashManager.uploadCrashes() succeeded, removing local crashes with ids ${crashes.joinToString { it.uuid }}" }
                    configuration.localRepo.removeCrashes(crashes.map(CrashUploadRequestBody.ApiCrash::uuid))

                    // All set and done. However, as we batch uploading, signal only success when we
                    // have not batched the crashes here (hasMore=false),
                    // and SuccessNeedsAnotherRun is hasMore=true.
                    if (hasMore) {
                        UploadLogicManager.TaskResult.SuccessNeedsAnotherRun
                    } else {
                        UploadLogicManager.TaskResult.Success
                    }
                } else {
                    KoarlLogger.log { "CrashManager.uploadCrashes failed, will try again later." }
                    //Error while uploading, return false to signal reschedule
                    UploadLogicManager.TaskResult.Failure
                }
            } else {
                //No crashes found -> return true to signal success
                UploadLogicManager.TaskResult.Success
            }
        }
    }

    companion object {
        /**
         * If too many crashes have occurred while being offline, we will get troubles when sending
         * too many crash items in one request (on client side - server-side can handle it quite
         * well, but room fetching and request-body transformation takes some resources).
         *
         * Therefore batch crash uploads in batches with at most 100 crashes per request.
         */
        private const val NUMBER_OF_BATCH_SEND_ITEMS = 100
    }

}