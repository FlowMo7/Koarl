package dev.moetz.koarl.backend.api.v1

import dev.moetz.koarl.api.model.CrashUploadRequestBody
import dev.moetz.koarl.backend.api.VersionedApi
import dev.moetz.koarl.backend.manager.CrashManager
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonException

class V1Api(
    private val json: Json,
    private val crashManager: CrashManager
) : VersionedApi {

    override val pathPrefix: String
        get() = "dev-v1"

    override fun install(route: Route) {
        route.apply {

            route("crash") {
                post {
                    val requestString = call.receiveText()

                    val request = try {
                        json.parse(CrashUploadRequestBody.serializer(), requestString)
                    } catch (e: JsonException) {
                        println("JsonException: ${e.localizedMessage}")
                        null
                    } catch (e: SerializationException) {
                        println("SerializationException: ${e.localizedMessage}")
                        null
                    }

                    if (request == null) {
                        call.respond(status = HttpStatusCode.BadRequest, message = "")
                    } else {
                        try {
                            crashManager.addCrashes(
                                deviceData = request.deviceData,
                                appData = request.appData,
                                crashes = request.crashes
                            )
                            call.respond(HttpStatusCode.NoContent, "")
                        } catch (throwable: Throwable) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                throwable.message ?: "No message available"
                            )
                        }
                    }
                }
            }
        }
    }

}