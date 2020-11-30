package dev.moetz.koarl.android

import android.content.Context
import androidx.annotation.RestrictTo
import dev.moetz.koarl.android.dsl.KoarlConfiguration
import dev.moetz.koarl.android.dsl.KoarlConfigurationBuilder
import dev.moetz.koarl.android.exceptionhandler.ExceptionManager

object Koarl {

    /**
     * Initialization which needs to be called in your Applications `onCreate` method.
     *
     * @param context The Applications context.
     * @param receiver a lambda to pass the library configuration (e.g. the server url) to.
     */
    @SuppressWarnings("unused")
    fun init(
        context: Context,
        receiver: KoarlConfiguration.Builder.() -> Unit
    ) {
        val configurationBuilder = KoarlConfiguration.Builder()
        receiver.invoke(configurationBuilder)

        initInternally(
            KoarlConfigurationBuilder.build(
                context,
                configurationBuilder
            )
        )
    }

    /**
     * Changes whether reporting should be enabled. Passing `false` here will disable all data
     * collection and data sending to the backend service.
     *
     * If data collection should be disabled globally since initialization if this library, please
     * consider using the [init] method to set the respective flag within the privacy settings there.
     * This method is aimed to be used for on-the-fly changes in enabling this library due to a user
     * consent change at runtime.
     *
     * @param reportingEnabled Whether this library should be enabled to store crash data and send
     * it to the backend service defined in the [init] method.
     */
    @SuppressWarnings("unused")
    fun reportingEnabled(reportingEnabled: Boolean) {
        InstanceProvider.changeReportingEnabled(reportingEnabled)
    }


    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private fun initInternally(config: KoarlConfiguration) {
        InstanceProvider.init(config)
        ExceptionManager.init()
        InstanceProvider.crashManager.init()
        InstanceProvider.lifecycleManager.init()
    }


    /**
     * Call to log a non-fatal exception.
     *
     * @param throwable The [Throwable] to log.
     */
    @SuppressWarnings("unused")
    fun logException(throwable: Throwable) {
        InstanceProvider.crashManager.addCrash(
            isFatal = false,
            throwable = throwable
        )
    }

}

/**
 * Marker annotation for DSL methods.
 */
@DslMarker
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.TYPE,
    AnnotationTarget.FUNCTION
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal annotation class ConfigurationDsl