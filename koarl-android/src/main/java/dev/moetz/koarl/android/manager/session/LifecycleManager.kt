package dev.moetz.koarl.android.manager.session

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import dev.moetz.koarl.android.KoarlLogger

internal class LifecycleManager {

    private val foregroundLifecycleObserver = ForegroundLifecycleObserver()

    val isInForeground: Boolean get() = foregroundLifecycleObserver.isInForeground

    fun init() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(foregroundLifecycleObserver)
    }


    /**
     * see https://developer.android.com/reference/androidx/lifecycle/ProcessLifecycleOwner.html
     */
    private inner class ForegroundLifecycleObserver : LifecycleObserver {

        var isInForeground: Boolean = false
            private set


        /**
         * ProcessLifecycleOwner will dispatch `Lifecycle.Event.ON_START`, `Lifecycle.Event.ON_RESUME`
         * events, as a first activity moves through these events.
         *
         * _https://developer.android.com/reference/androidx/lifecycle/ProcessLifecycleOwner.html_
         */
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onSessionStart() {
            KoarlLogger.log { "onSessionStart()" }
            isInForeground = true
        }


        /**
         *
         * Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, events will be dispatched with a delay
         * after a last activity passed through them. This delay is long enough to guarantee that
         * ProcessLifecycleOwner won't send any events if activities are destroyed and recreated
         * due to a configuration change.
         *
         * _https://developer.android.com/reference/androidx/lifecycle/ProcessLifecycleOwner.html_
         */
        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onSessionStop() {
            KoarlLogger.log { "onSessionStop()" }
            isInForeground = false
        }

    }

}