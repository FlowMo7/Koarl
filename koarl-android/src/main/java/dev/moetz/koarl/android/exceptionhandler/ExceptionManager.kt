package dev.moetz.koarl.android.exceptionhandler

import androidx.annotation.RestrictTo
import dev.moetz.koarl.android.InstanceProvider

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object ExceptionManager {

    fun init() {
        Thread.setDefaultUncaughtExceptionHandler(
            getUncaughtExceptionHandler(
                Thread.getDefaultUncaughtExceptionHandler()
            )
        )
    }

    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        InstanceProvider.crashManager.addCrash(
            isFatal = true,
            throwable = throwable
        )
    }

    private fun getUncaughtExceptionHandler(uncaughtExceptionHandler: Thread.UncaughtExceptionHandler?): Thread.UncaughtExceptionHandler {
        return Thread.UncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(thread, throwable)
            uncaughtExceptionHandler?.uncaughtException(thread, throwable)
        }
    }


}