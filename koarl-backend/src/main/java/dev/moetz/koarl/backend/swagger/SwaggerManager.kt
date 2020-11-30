package dev.moetz.koarl.backend.swagger

import dev.moetz.koarl.backend.authentication.BasicAuthenticationManager
import dev.moetz.koarl.backend.environment.EnvironmentVariable
import dev.moetz.swagger.SwaggerHandler
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CachingHeaders
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.CachingOptions
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import org.koin.dsl.module
import java.util.*

val swaggerModule = module {
    single {
        val swaggerHandler = SwaggerHandler(
            swaggerDefinition = SwaggerDefinitionProvider.get(),
            hideSwaggerValidator = true,
            hideSwaggerUrlField = true
        )

        SwaggerManager(
            swaggerHandler = swaggerHandler,
            basicAuthenticationManager = get(),
            environmentVariable = get()
        )
    }
}

class SwaggerManager(
    private val swaggerHandler: SwaggerHandler,
    private val basicAuthenticationManager: BasicAuthenticationManager,
    private val environmentVariable: EnvironmentVariable
) {

    fun install(route: Route) {
        if (environmentVariable.getBoolean(EnvironmentVariable.Key.Swagger.Enable) == true) {
            basicAuthenticationManager.basicAuthentication(
                route,
                BasicAuthenticationManager.Realm.Swagger
            ) {

                install(CachingHeaders) {
                    options { outgoingContent ->
                        val contentType = outgoingContent.contentType?.withoutParameters()
                        //only everything but the YML swagger definition
                        if (contentType?.contentType?.toLowerCase(Locale.ENGLISH) == "application" &&
                            (contentType.contentSubtype.contains("yaml", ignoreCase = true) ||
                                    contentType.contentSubtype.contains("yml", ignoreCase = true))
                        ) {
                            null
                        } else {
                            CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 60 * 60 * 24))
                        }
                    }
                }

                get("{...}") {
                    val relativeUri = call.request.uri.substringAfter("swagger")

                    if (relativeUri == "") {
                        call.respondRedirect("swagger/", true)
                    } else {
                        val uri = relativeUri
                            .removePrefixedSlash()
                            .toIndexHtmlIfBlank()

                        val pair = swaggerHandler.get(uri)

                        if (pair != null) {
                            val (mimeType, byteArray) = pair
                            call.respondBytes(ContentType.parse(mimeType)) { byteArray }
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }
            }
        }
    }

    private fun String.toIndexHtmlIfBlank(): String {
        return this.let {
            if (it.isBlank()) "index.html" else it
        }
    }

    private fun String.removePrefixedSlash(): String {
        return this.let {
            if (it.firstOrNull() == '/') it.drop(1) else it
        }
    }
}