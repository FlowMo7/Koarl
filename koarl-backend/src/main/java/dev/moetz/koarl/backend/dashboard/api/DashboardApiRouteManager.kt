package dev.moetz.koarl.backend.dashboard.api

import dev.moetz.koarl.backend.dashboard.MaterialDashboard
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route

class DashboardApiRouteManager(
    private val dashboardApi: DashboardApi
) {
    fun install(route: Route) {
        route.route("api") {

            route("app") {

                route("{packageName}") {

                    route("obfuscation") {

                        route("{versionCode}") {

                            route("mapping") {

                                get {
                                    val packageName = call.parameters["packageName"]
                                    val versionCode =
                                        call.parameters["versionCode"]?.toLongOrNull()

                                    if (packageName.isNullOrBlank() || versionCode == null) {
                                        call.respond(
                                            status = HttpStatusCode.BadRequest,
                                            message = "{packageName} or {versionCode} not set"
                                        )
                                    } else {
                                        val mapping = dashboardApi.getObfuscationMapping(
                                            packageName = packageName,
                                            appVersionCode = versionCode
                                        )

                                        if (mapping != null) {
                                            call.respondText(
                                                contentType = ContentType.Text.Plain,
                                                status = HttpStatusCode.OK,
                                                text = mapping
                                            )
                                        } else {
                                            call.respond(
                                                status = HttpStatusCode.NotFound,
                                                message = ""
                                            )
                                        }
                                    }
                                }

                                post {
                                    val packageName = call.parameters["packageName"]
                                    val versionCode =
                                        call.parameters["versionCode"]?.toLongOrNull()

                                    if (packageName.isNullOrBlank() || versionCode == null) {
                                        call.respond(
                                            status = HttpStatusCode.BadRequest,
                                            message = "{packageName} or {versionCode} not set"
                                        )
                                    } else {
                                        val requestString = call.receiveText()

                                        try {
                                            dashboardApi.addObfuscationMapping(
                                                packageName = packageName,
                                                appVersionCode = versionCode,
                                                mappingFileContents = requestString
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

                        get {
                            val packageName = call.parameters["packageName"]

                            if (packageName.isNullOrBlank()) {
                                call.respond(
                                    status = HttpStatusCode.BadRequest,
                                    message = "{packageName} not set"
                                )
                            } else {
                                try {
                                    val response = dashboardApi.getObfuscationMappingVersionCodes(
                                        packageName = packageName
                                    )
                                    call.respond(
                                        status = HttpStatusCode.OK,
                                        message = response
                                    )
                                } catch (throwable: Throwable) {
                                    throwable.printStackTrace()
                                }
                            }
                        }
                    }

                    route("group") {

                        // "api/app/{packageName}/group"
                        get {

                            val packageName = call.parameters["packageName"]
                            val typeParameter = call.parameters["type"].let { parameter ->
                                when(parameter) {
                                    "fatal" -> DashboardApi.CrashType.OnlyFatal
                                    "nonfatal" -> DashboardApi.CrashType.OnlyNonFatal
                                    else -> DashboardApi.CrashType.All
                                }
                            }

                            if (packageName.isNullOrBlank()) {
                                call.respond(
                                    status = HttpStatusCode.BadRequest,
                                    message = "{packageName} not set"
                                )
                            } else {
                                val response = dashboardApi.getCrashGroupsForApp(
                                    packageName = packageName,
                                    crashType = typeParameter
                                )

                                if (response != null) {
                                    call.respond(
                                        status = HttpStatusCode.OK,
                                        message = response
                                    )
                                } else {
                                    call.respond(
                                        status = HttpStatusCode.NotFound,
                                        message = ""
                                    )
                                }
                            }
                        }

                        route("{groupId}") {

                            route("crash") {

                                // "api/app/{packageName}/group/{groupId}/crash"
                                get {
                                    val packageName = call.parameters["packageName"]
                                    val groupId = call.parameters["groupId"]

                                    if (packageName.isNullOrBlank() || groupId.isNullOrBlank()) {
                                        call.respond(
                                            status = HttpStatusCode.BadRequest,
                                            message = "{packageName} or {groupId} not set"
                                        )
                                    } else {
                                        val response = dashboardApi.getGroupCrashes(
                                            packageName = packageName,
                                            groupId = groupId
                                        )

                                        if (response != null) {
                                            call.respond(
                                                status = HttpStatusCode.OK,
                                                message = response
                                            )
                                        } else {
                                            call.respond(
                                                status = HttpStatusCode.NotFound,
                                                message = ""
                                            )
                                        }
                                    }
                                }

                                // "api/app/{packageName}/group/{groupId}/crash/{crashId}"
                                get("{crashId}") {
                                    val packageName = call.parameters["packageName"]
                                    val groupId = call.parameters["groupId"]
                                    val crashId = call.parameters["crashId"]

                                    if (packageName.isNullOrBlank() || groupId.isNullOrBlank() || crashId.isNullOrBlank()) {
                                        call.respond(
                                            status = HttpStatusCode.BadRequest,
                                            message = "{packageName}, {crashId} or {groupId} not set"
                                        )
                                    } else {
                                        val response = dashboardApi.getCrashInGroup(
                                            packageName = packageName,
                                            groupId = groupId,
                                            crashId = crashId
                                        )

                                        if (response != null) {
                                            call.respond(
                                                status = HttpStatusCode.OK,
                                                message = response
                                            )
                                        } else {
                                            call.respond(
                                                status = HttpStatusCode.NotFound,
                                                message = ""
                                            )
                                        }
                                    }
                                }
                            }

                        }
                    }
                }

                // "api/app"
                get {
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = dashboardApi.getAllApps()
                    )
                }
            }
        }
    }
}