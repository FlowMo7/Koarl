package dev.moetz.koarl.backend.api

import io.ktor.routing.Route

interface VersionedApi {

    val pathPrefix: String

    fun install(route: Route)

}