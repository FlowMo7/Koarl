package dev.moetz.koarl.android

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun launchInProcessLifecycleScope(block: suspend CoroutineScope.() -> Unit) {
    ProcessLifecycleOwner.get().lifecycleScope.launch {
        block()
    }
}

const val libraryVersionName: String = dev.moetz.koarl.android.BuildConfig.LIBRARY_VERSION_NAME