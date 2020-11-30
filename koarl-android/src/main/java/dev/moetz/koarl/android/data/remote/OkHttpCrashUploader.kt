package dev.moetz.koarl.android.data.remote

import dev.moetz.koarl.android.KoarlLogger
import dev.moetz.koarl.android.libraryVersionName
import dev.moetz.koarl.api.model.ApiAppData
import dev.moetz.koarl.api.model.ApiDeviceData
import dev.moetz.koarl.api.model.CrashUploadRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.coroutines.resume

class OkHttpCrashUploader(
    private val json: Json,
    okHttpClient: OkHttpClient
) : CrashUploader {

    private val client: OkHttpClient = okHttpClient.newBuilder()
        .addInterceptor(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                return chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("X-Library-Version", libraryVersionName)
                        .addHeader("User-Agent", "Koarl-Android $libraryVersionName")
                        .build()
                )
            }
        })
        .build()


    override suspend fun uploadCrashes(
        baseUrl: String,
        deviceData: ApiDeviceData?,
        appData: ApiAppData,
        crashes: List<CrashUploadRequestBody.ApiCrash>
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val body = json.stringify(
                CrashUploadRequestBody.serializer(),
                CrashUploadRequestBody(
                    deviceData = deviceData,
                    appData = appData,
                    crashes = crashes
                )
            )

            suspendCancellableCoroutine<Boolean> { continuation ->
                val request = client.newCall(
                    Request.Builder()
                        .url(baseUrl + "api/$API_VERSION_PATH/crash")
                        .post(body.toRequestBody("application/json".toMediaTypeOrNull()))
                        .build()
                )
                request.enqueue(object : Callback {

                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response.isSuccessful)
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        KoarlLogger.log { "Error uploading crashes. ${e.message}" }
                        continuation.resume(false)
                    }

                })

                continuation.invokeOnCancellation {
                    request.cancel()
                }
            }
        }
    }


    companion object {
        private const val API_VERSION_PATH = "dev-v1"
    }

}