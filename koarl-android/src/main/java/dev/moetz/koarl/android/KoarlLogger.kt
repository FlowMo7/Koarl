package dev.moetz.koarl.android

import android.util.Log
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object KoarlLogger {


    @PublishedApi
    internal var enabled: Boolean = false
        private set

    fun setLoggingEnabled(enabled: Boolean) {
        this.enabled = enabled
    }


    inline fun log(messageProvider: () -> String) {
        if (enabled) {
            Log.d("Koarl", messageProvider.invoke())
        }
    }


}