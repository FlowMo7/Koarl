package dev.moetz.koarl.android.dsl

import android.content.Context
import android.os.Build
import dev.moetz.koarl.android.KoarlLogger
import dev.moetz.koarl.android.data.local.LocalRepo
import dev.moetz.koarl.android.data.local.RoomLocalRepo
import dev.moetz.koarl.android.data.remote.CrashUploader
import dev.moetz.koarl.android.data.remote.OkHttpCrashUploader
import dev.moetz.koarl.android.device.AndroidDeviceStateRetriever
import dev.moetz.koarl.android.device.DeviceStateRetriever
import dev.moetz.koarl.api.model.ApiAppData
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@PublishedApi
internal object KoarlConfigurationBuilder {

    @PublishedApi
    internal fun build(
        context: Context,
        builder: KoarlConfiguration.Builder
    ): KoarlConfiguration {
        val baseUrl = requireNotNull(builder.baseUrl) {
            "url not provided. You need at least call 'baseUrl(\"https://your.koarl.url\")' on the DSL of 'Koarl.init()'."
        }

        KoarlLogger.setLoggingEnabled(builder.debugLogsEnabled ?: false)

        val appData = getAppData(builder, context)

        if (KoarlLogger.enabled) {
            KoarlLogger.log { "\n---- Koarl KoarlConfiguration ----\n" }
            KoarlLogger.log { "[BaseUrl]              ${builder.baseUrl}\n" }
            KoarlLogger.log { "[AppName]              ${appData.appName}\n" }
            KoarlLogger.log { "[AppVersion]           ${appData.appVersionName} (${appData.appVersionCode})\n" }

            if (builder.localRepo != null) {
                KoarlLogger.log { "[LocalRepo]            Using provided ${builder.localRepo}\n" }
            } else {
                KoarlLogger.log { "[LocalRepo]            Using built-in\n" }
            }

            if (builder.crashUploader != null) {
                KoarlLogger.log { "[CrashUploader]        Using provided ${builder.crashUploader}\n" }
            } else {
                KoarlLogger.log { "[CrashUploader]        Using built-in\n" }
            }

            if (builder.certificatePinner != null) {
                KoarlLogger.log { "[CertificatePinner]    Using provided ${builder.certificatePinner}\n" }
                if (builder.crashUploader != null) {
                    KoarlLogger.log { "[CertificatePinner]    WARNING! The provided CertificatePinner will not have any effect, as a custom CrashUploader is provided. Implement your certificate pinning in your custom CrashUploader!\n" }
                }
            }

            if (builder.deviceStateRetriever != null) {
                KoarlLogger.log { "[DeviceStateRetriever] Using provided ${builder.deviceStateRetriever}\n" }
            } else {
                KoarlLogger.log { "[DeviceStateRetriever] Using built-in\n" }
            }


            val builtPrivacySettings = builder.privacySettings.build()
            KoarlLogger.log { "[PrivacySettings]      sendDeviceData: ${builtPrivacySettings.sendDeviceData}\n" }
            KoarlLogger.log { "[PrivacySettings]      enableReporting: ${builtPrivacySettings.enableReporting}\n" }

            val builtTimingSettings = builder.timingSettings.build()
            KoarlLogger.log { "[TimingSettings]       delayAfterApplicationStartToUpload: ${builtTimingSettings.delayAfterApplicationStartToUpload}\n" }
            KoarlLogger.log { "[TimingSettings]       delayToRetryUpload: ${builtTimingSettings.delayToRetryUpload}\n" }

            KoarlLogger.log { "---- Koarl KoarlConfiguration ----" }
        }

        return KoarlConfiguration(
            baseUrl = baseUrl.let { url -> if (url.endsWith('/').not()) "$url/" else url },
            localRepo = getLocalRepo(builder, context),
            crashUploader = getUploader(builder),
            deviceStateRetriever = getDeviceStateRetriever(builder, context),
            appData = appData,
            privacySettings = builder.privacySettings.build(),
            timingSettings = builder.timingSettings.build()
        )
    }


    private fun getUploader(builder: KoarlConfiguration.Builder): CrashUploader {
        return builder.crashUploader ?: OkHttpCrashUploader(
            json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = false
            },
            okHttpClient = OkHttpClient()
                .newBuilder()
                .apply {
                    builder.certificatePinner?.let { certificatePinner ->
                        certificatePinner(certificatePinner)
                    }
                }
                .build()
        )
    }

    private fun getLocalRepo(builder: KoarlConfiguration.Builder, context: Context): LocalRepo {
        return builder.localRepo ?: RoomLocalRepo(context)
    }

    private fun getDeviceStateRetriever(
        builder: KoarlConfiguration.Builder,
        context: Context
    ): DeviceStateRetriever {
        return builder.deviceStateRetriever ?: AndroidDeviceStateRetriever(context)
    }


    private fun getAppData(builder: KoarlConfiguration.Builder, context: Context): ApiAppData {
        val appVersion = if (builder.appVersion != null) {
            builder.appVersion!!
        } else {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            val versionName = packageInfo.versionName.orEmpty()

            versionCode to versionName
        }

        return ApiAppData(
            packageName = context.packageName,
            appName = builder.appName ?: getApplicationName(context),
            appVersionCode = appVersion.first,
            appVersionName = appVersion.second
        )
    }

    private fun getApplicationName(context: Context): String {
        val applicationInfo = context.applicationInfo
        return applicationInfo.labelRes
            .takeIf { it != 0 }
            ?.let { context.getString(it) }
            ?: applicationInfo.nonLocalizedLabel.toString()
    }

}