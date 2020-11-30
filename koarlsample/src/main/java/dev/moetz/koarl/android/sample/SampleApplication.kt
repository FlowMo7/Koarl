package dev.moetz.koarl.android.sample

import android.app.Application
import dev.moetz.koarl.android.Koarl
import dev.moetz.koarl.android.timber.KoarlTimberTree
import okhttp3.CertificatePinner
import timber.log.Timber

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Koarl.init(context = this) {
            baseUrl("https://koarl.dev/")

            debugLogsEnabled(true)

            privacySettings {
                sendDeviceData(true)
            }

            certificatePinner(
                CertificatePinner.Builder()
                    .add("koarl.dev", "sha256/YLh1dUR9y6Kja30RrAn7JKnbQG/uEtLMkBgFF2Fuihg=")
                    .build()
            )
        }

        Timber.plant(KoarlTimberTree())
    }

}