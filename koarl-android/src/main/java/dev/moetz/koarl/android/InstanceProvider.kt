package dev.moetz.koarl.android

import androidx.annotation.RestrictTo
import dev.moetz.koarl.android.dsl.KoarlConfiguration
import dev.moetz.koarl.android.manager.CrashManager
import dev.moetz.koarl.android.manager.session.LifecycleManager

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object InstanceProvider {

    private lateinit var configuration: KoarlConfiguration

    internal fun init(configuration: KoarlConfiguration) {
        this.configuration = configuration
    }

    internal fun changeReportingEnabled(reportingEnabled: Boolean) {
        this.configuration = configuration.copy(
            privacySettings = configuration.privacySettings.copy(
                enableReporting = reportingEnabled
            )
        )
    }

    internal val crashManager: CrashManager by lazy {
        CrashManager(lifecycleManager) { configuration }
    }

    internal val lifecycleManager: LifecycleManager by lazy {
        LifecycleManager()
    }


}