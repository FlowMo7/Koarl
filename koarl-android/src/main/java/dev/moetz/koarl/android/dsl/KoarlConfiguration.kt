package dev.moetz.koarl.android.dsl

import android.content.Context
import dev.moetz.koarl.android.ConfigurationDsl
import dev.moetz.koarl.android.data.local.LocalRepo
import dev.moetz.koarl.android.data.local.RoomLocalRepo
import dev.moetz.koarl.android.data.remote.CrashUploader
import dev.moetz.koarl.android.data.remote.OkHttpCrashUploader
import dev.moetz.koarl.android.device.AndroidDeviceStateRetriever
import dev.moetz.koarl.android.device.DeviceStateRetriever
import dev.moetz.koarl.api.model.ApiAppData
import okhttp3.CertificatePinner


data class KoarlConfiguration internal constructor(
    internal val baseUrl: String,
    internal val localRepo: LocalRepo,
    internal val crashUploader: CrashUploader,
    internal val deviceStateRetriever: DeviceStateRetriever,
    internal val appData: ApiAppData,
    internal val privacySettings: PrivacySettings,
    internal val timingSettings: TimingSettings
) {

    class Builder @PublishedApi internal constructor(
        internal var baseUrl: String? = null,
        internal var localRepo: LocalRepo? = null,
        internal var crashUploader: CrashUploader? = null,
        internal var certificatePinner: CertificatePinner? = null,
        internal var deviceStateRetriever: DeviceStateRetriever? = null,
        internal var debugLogsEnabled: Boolean? = null,
        internal var appVersion: Pair<Long, String>? = null,
        internal var appName: String? = null,
        internal var privacySettings: PrivacySettings.Builder = PrivacySettings.Builder(),
        internal var timingSettings: TimingSettings.Builder = TimingSettings.Builder()
    ) {


        /**
         * The base-url to upload the crash reports to.
         *
         * It is mandatory to set the base-url.
         *
         * @param baseUrl the base-url to upload the crashes to
         */
        @Suppress("unused")
        @ConfigurationDsl
        fun baseUrl(baseUrl: String) {
            this.baseUrl = baseUrl
        }

        /**
         * Whether verbose debug log messages should be printed into the logcat.
         *
         * This **should not** be enabled for production-builds, and should be enabled in
         * debug-builds **only** when you want to know what's exactly going on, as it will flood
         * your devices' log.
         *
         * Note, that this does not enable/disable the logging to the backend, but only debug
         * message output to the logcat.
         *
         * @param debugLogsEnabled whether the library should output debug-logs into the logcat.
         */
        @Suppress("unused")
        @ConfigurationDsl
        fun debugLogsEnabled(debugLogsEnabled: Boolean) {
            this.debugLogsEnabled = debugLogsEnabled
        }

        /**
         * Whether a custom (given) [LocalRepo] should be used to store the crashes locally.
         *
         * If no [LocalRepo] is provided, then the default [RoomLocalRepo] will be used.
         *
         * @param localRepo The custom [LocalRepo] to use to store the crashes locally.
         */
        @Suppress("unused")
        @ConfigurationDsl
        fun customLocalRepo(localRepo: LocalRepo) {
            this.localRepo = localRepo
        }

        /**
         * Whether a custom (given) [CrashUploader] should be used to upload the crashes.
         *
         * If no [CrashUploader] it provided, then the default [OkHttpCrashUploader] will be used.
         *
         * @param crashUploader The custom [CrashUploader] to use to upload the crashes.
         */
        @Suppress("unused")
        @ConfigurationDsl
        fun customCrashUploader(crashUploader: CrashUploader) {
            this.crashUploader = crashUploader
        }


        /**
         * Whether TLS connections to the backend should be pinned to certificates (in order to improve security).
         *
         * **Note:** If yu are using a custom [CrashUploader] (defined using [customCrashUploader]),
         * this certificate pinner **will not be used**.
         *
         * In that case, you need to implement your certificate pinning mechanism in your
         * custom [CrashUploader].
         *
         * @param certificatePinner The [CertificatePinner] to use to pin SSL connections.
         */
        @Suppress("unused")
        @ConfigurationDsl
        fun certificatePinner(certificatePinner: CertificatePinner) {
            this.certificatePinner = certificatePinner
        }

        /**
         * Whether a custom (given) [DeviceStateRetriever] should be used to get the device details.
         *
         * If no [DeviceStateRetriever] it provided, then the default [AndroidDeviceStateRetriever] will be used.
         *
         * @param deviceStateRetriever The custom [DeviceStateRetriever] to use to get the device state.
         */
        @Suppress("unused")
        @ConfigurationDsl
        fun deviceStateRetriever(deviceStateRetriever: DeviceStateRetriever) {
            this.deviceStateRetriever = deviceStateRetriever
        }

        /**
         * The name of this application. If not set, will be taken from [Context.getApplicationInfo].
         * @param appName The custom application name to set.
         */
        @Suppress("unused")
        @ConfigurationDsl
        fun appName(appName: String) {
            this.appName = appName
        }

        /**
         * The version of this application.
         * If not set customly, will be taken from te application's config.
         *
         * @param versionCode The version code as [Long].
         * @param versionString The version string as [String].
         */
        @Suppress("unused")
        @ConfigurationDsl
        fun version(versionCode: Long, versionString: String) {
            this.appVersion = versionCode to versionString
        }

        /**
         * DSL method to set privacy-related settings.
         *
         * @see PrivacySettings.Builder
         */
        @Suppress("unused")
        @ConfigurationDsl
        fun privacySettings(settings: PrivacySettings.Builder.() -> Unit) {
            settings.invoke(this.privacySettings)
        }

        /**
         * DSL method to set timing-related settings.
         *
         * @see TimingSettings.Builder
         */
        @Suppress("unused")
        @ConfigurationDsl
        fun timingSettings(settings: TimingSettings.Builder.() -> Unit) {
            settings.invoke(this.timingSettings)
        }


    }
}