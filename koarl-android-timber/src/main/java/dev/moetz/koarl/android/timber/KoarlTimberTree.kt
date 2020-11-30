package dev.moetz.koarl.android.timber

import android.util.Log
import dev.moetz.koarl.android.Koarl
import timber.log.Timber

class KoarlTimberTree(
    private val shouldLog: (tag: String?, priority: Int) -> Boolean = { _, priority -> priority == Log.ERROR || priority == Log.WARN }
) : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return shouldLog.invoke(tag, priority)
    }


    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Also log the message sent to Timber (e.g. `Timber.e(throwable, "Some error at xyz")`,
        // as it may contain useful information.
        // It will be logged as a custom "log" to the exception, which is visible at Firebase in
        // the "Logs" tab of the exception.
        // Timber, however, attaches the whole stacktrace onto the message, separated by a
        // line-break (\n), which is why the message is cut off at the first line-break (if one found)
        //TODO!
        //Crashlytics.log(customMessage(priority, message.substringBefore('\n')))

        val throwable = t ?: KoarlException(customMessage(priority, message))
        Koarl.logException(throwable)
    }

    private fun customMessage(priority: Int, message: String): String = when (priority) {
        Log.VERBOSE -> "VERBOSE"
        Log.DEBUG -> "DEBUG"
        Log.INFO -> "INFO"
        Log.WARN -> "WARN"
        Log.ERROR -> "ERROR"
        Log.ASSERT -> "ASSERT"
        else -> "NO_PRIORITY"
    }.let { "$it: $message" }

    private inner class KoarlException(message: String, cause: Throwable? = null) :
        Throwable(message, cause) {
        @Synchronized
        override fun fillInStackTrace(): Throwable {
            super.fillInStackTrace()

            val ignoredClassNames =
                listOf(Timber::class.java.simpleName, KoarlTimberTree::class.java.simpleName)

            stackTrace = stackTrace.asSequence()
                .filterNot {
                    it.fileName != null && ignoredClassNames.any { name ->
                        it.fileName.contains(
                            name
                        )
                    }
                }
                .filterNot {
                    it.className != null && ignoredClassNames.any { name ->
                        it.className.contains(
                            name
                        )
                    }
                }
                .toList()
                .toTypedArray()

            return this
        }
    }
}