package dev.moetz.koarl.android.manager

import dev.moetz.koarl.android.KoarlLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex

internal class UploadLogicManager {

    @PublishedApi
    internal val isCurrentlyUploading: Mutex = Mutex(false)


    /**
     * The result of a task, especially in context of what should be done after the task has been
     * executed.
     */
    enum class TaskResult {
        /**
         * Indicates that the task has been performed successful (or not at all if there are e.g.
         * no crashes to upload), and there are no more items to be sent to the server (as far as
         * the task performer is informed of).
         */
        Success,
        /**
         * Indicates that the task has been performed successful, and that there are more crashes
         * to be uploaded, so another task execution should be scheduled.
         */
        SuccessNeedsAnotherRun,
        /**
         * Indicates that the task has not been performed successfully, and that the task should be
         * rescheduled after a given delay.
         */
        Failure
    }

    /**
     * Invokes the given [uploadingTask] in a synchronized manner (mutual exclusion, only one at a
     * time possible), and handles errors with retries after the given [retryDelay] (and also delays
     * for [retryDelay] and re-invokes the [uploadingTask] if the method is invoked while a former
     * [uploadingTask] is still in progress)
     *
     * @param retryDelay The amount of milliseconds to wait before retrying the [uploadingTask]
     * @param uploadingTask The lambda which is invoked which performs the uploading task.
     * The [TaskResult] item responded from the [uploadingTask] indicates the result of the task.
     */
    suspend inline fun uploadAndRetryLogic(retryDelay: Long, uploadingTask: () -> TaskResult) {
        KoarlLogger.log { "UploadLogicManager.uploadAndRetryLogic()" }
        val canUpload = isCurrentlyUploading.tryLock()
        try {
            if (canUpload) {
                do {
                    val uploadTaskResult = uploadingTask.invoke()
                    KoarlLogger.log { "Uploading responded with $uploadTaskResult" }
                    val taskResult: TaskResult? = uploadTaskResult


                    val shouldRerun: Boolean = when (taskResult) {
                        TaskResult.Success -> {
                            //Success, don't need to rerun
                            false
                        }
                        TaskResult.SuccessNeedsAnotherRun -> {
                            //Success, but has more data to upload. Instant rerun
                            true
                        }
                        TaskResult.Failure -> {
                            //Failure, rerun after delay
                            KoarlLogger.log { "Upload scheduled for retry in $retryDelay ms." }
                            //TODO delay within mutex section
                            delay(retryDelay)
                            KoarlLogger.log { "Retrying upload now (after waiting ${retryDelay} ms" }
                            true
                        }
                        null -> {
                            //Another thread is currently working the uploadTask, we have indicated that we want to upload something.
                            false
                        }
                    }
                } while (shouldRerun)
            } else {
                // uploading still in progress, but it seems that there has been added another crash.
                // the new crash should be uploaded afterwards, so return here that we need to retry after delay
                KoarlLogger.log { "Uploading still in progress. Signalling uploading thread to retry after delay." }
            }
        } finally {
            if (canUpload) {
                isCurrentlyUploading.unlock()
            }
        }
    }
}