package dev.moetz.koarl.backend

import dev.moetz.koarl.backend.api.v1.V1Api
import dev.moetz.koarl.backend.authentication.BasicAuthenticationManager
import dev.moetz.koarl.backend.dashboard.DashboardManager
import dev.moetz.koarl.backend.environment.EnvironmentVariable
import dev.moetz.koarl.backend.grafana.GrafanaManager
import dev.moetz.koarl.backend.grafana.grafanaModule
import dev.moetz.koarl.backend.koin.apiModule
import dev.moetz.koarl.backend.koin.appModule
import dev.moetz.koarl.backend.koin.databaseModule
import dev.moetz.koarl.backend.swagger.SwaggerManager
import dev.moetz.koarl.backend.swagger.swaggerModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.inject
import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE
import kotlin.system.exitProcess

object Main : KoinComponent {

    private val swaggerManager: SwaggerManager by inject()
    private val dashboardManager: DashboardManager by inject()
    private val grafanaManager: GrafanaManager by inject()
    private val basicAuthenticationManager: BasicAuthenticationManager by inject()
    private val environmentVariable: EnvironmentVariable by inject()
    private val v1Api: V1Api by inject()
    private val json: Json by inject()

    @JvmStatic
    fun main(args: Array<String>) {
        startKoin {
            modules(
                appModule,
                databaseModule,
                apiModule,
                grafanaModule,
                swaggerModule
            )
            logger(object : Logger(Level.ERROR) {
                override fun log(level: Level, msg: MESSAGE) = println("[${level.name}]: $msg")
            })
        }

        val address = environmentVariable.require(EnvironmentVariable.Key.Application.Address)
        val port = environmentVariable.require(EnvironmentVariable.Key.Application.Port).toInt()

        if (args.firstOrNull() == "healthcheck") {
            healthcheck(address, port)
        } else {
            application(address, port)
        }
    }

    private fun application(address: String, port: Int) {

        println("Application listening at $address:$port")

        embeddedServer(factory = Netty, host = address, port = port) {
            install(ContentNegotiation) {
                json(json = json, contentType = ContentType.Application.Json)
            }

            install(Authentication) {
                basicAuthenticationManager.install(this)
            }

            routing {

                get("healthcheck") {
                    call.respond(HttpStatusCode.OK, "Ok")
                }

                route("api") {
                    route(v1Api.pathPrefix, v1Api::install)
                }

                route("dashboard") {
                    dashboardManager.install(this)
                }

                route("swagger") {
                    swaggerManager.install(this)
                }

                route("grafana-api") {
                    grafanaManager.installRoute(this)
                }
            }
        }.start(wait = true)
    }

    private fun healthcheck(address: String, port: Int) {
        val client = HttpClient(OkHttp)
        try {
            runBlocking {
                client.get<String>("http://$address:$port/healthcheck")
            }
            exitProcess(0)
        } catch (exception: Exception) {
            exception.printStackTrace()
            exitProcess(-1)
        }
    }


}