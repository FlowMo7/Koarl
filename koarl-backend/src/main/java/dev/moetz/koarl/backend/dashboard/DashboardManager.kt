package dev.moetz.koarl.backend.dashboard

import dev.moetz.koarl.backend.authentication.BasicAuthenticationManager
import dev.moetz.koarl.backend.dashboard.api.DashboardApi
import dev.moetz.koarl.backend.dashboard.api.DashboardApiRouteManager
import dev.moetz.koarl.backend.environment.EnvironmentVariable
import dev.moetz.koarl.backend.environment.dashboardUrl
import dev.moetz.koarl.backend.manager.CrashManager
import dev.moetz.koarl.backend.obfuscation.ObfuscationManager
import dev.moetz.koarl.backend.persistence.AppStorage
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CachingHeaders
import io.ktor.features.ConditionalHeaders
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.*
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.utils.io.core.readText
import java.util.concurrent.TimeUnit

class DashboardManager(
    private val crashManager: CrashManager,
    private val appStorage: AppStorage,
    private val basicAuthenticationManager: BasicAuthenticationManager,
    private val dashboardApiRouteManager: DashboardApiRouteManager,
    private val materialDashboard: MaterialDashboard,
    private val obfuscationManager: ObfuscationManager,
    private val environmentVariable: EnvironmentVariable
) {

    fun install(route: Route) {
        route.apply {
            basicAuthenticationManager.basicAuthentication(
                this,
                BasicAuthenticationManager.Realm.Dashboard
            ) {

                get {
                    call.respondText(
                        contentType = ContentType.Text.Html,
                        status = HttpStatusCode.OK,
                        provider = {
                            materialDashboard.mainPage()
                        }
                    )
                }

                route("app/{packageName}") {

                    get {
                        val appParameter = call.appParameter()
                        val typeParameter = call.parameters["type"].let { parameter ->
                            when (parameter) {
                                DashboardApi.CrashType.OnlyFatal.key -> DashboardApi.CrashType.OnlyFatal
                                DashboardApi.CrashType.OnlyNonFatal.key -> DashboardApi.CrashType.OnlyNonFatal
                                null, "", DashboardApi.CrashType.All.key -> DashboardApi.CrashType.All
                                else -> null
                            }
                        }

                        if (appParameter == null) {
                            materialDashboard.errorPage(
                                call,
                                HttpStatusCode.NotFound,
                                "Not found",
                                "App not found."
                            )
                        } else if (typeParameter == null) {
                            materialDashboard.errorPage(
                                call,
                                HttpStatusCode.BadRequest,
                                "Bad Request",
                                "'type' parameter invalid. Valid options: " +
                                        "${DashboardApi.CrashType.values().map { "'${it.key}'" }}"
                            )
                        } else {
                            call.respondText(
                                contentType = ContentType.Text.Html,
                                status = HttpStatusCode.OK,
                                provider = {
                                    materialDashboard.appPage(
                                        packageName = appParameter.packageName,
                                        appName = appParameter.appName,
                                        page = call.parameters["page"]?.toIntOrNull() ?: 1,
                                        limit = call.parameters["limit"]?.toIntOrNull(),
                                        crashTypes = typeParameter
                                    )
                                }
                            )
                        }
                    }

                    get("group/{groupId}") {
                        val appParameter = call.appParameter()

                        if (appParameter == null) {
                            materialDashboard.errorPage(
                                call,
                                HttpStatusCode.NotFound,
                                "Not found",
                                "App not found."
                            )
                        } else {
                            val groupParameter = call.groupParameter(appParameter.packageName)

                            if (groupParameter == null) {
                                materialDashboard.errorPage(
                                    call,
                                    HttpStatusCode.NotFound,
                                    "Not found",
                                    "Group not found."
                                )
                            } else {
                                call.respondText(
                                    contentType = ContentType.Text.Html,
                                    status = HttpStatusCode.OK,
                                    provider = {
                                        materialDashboard.groupPage(
                                            packageName = appParameter.packageName,
                                            appName = appParameter.appName,
                                            crashes = groupParameter.crashes,
                                            group = groupParameter.group,
                                            currentPage = call.parameters["page"]?.toIntOrNull()
                                                ?: 1
                                        )
                                    }
                                )
                            }
                        }
                    }

                    get("group/{groupId}/crash/{crashId}") {
                        val appParameter = call.appParameter()

                        if (appParameter == null) {
                            materialDashboard.errorPage(
                                call,
                                HttpStatusCode.NotFound,
                                "Not found",
                                "App not found."
                            )
                        } else {
                            val groupParameter = call.groupParameter(appParameter.packageName)

                            if (groupParameter == null) {
                                materialDashboard.errorPage(
                                    call,
                                    HttpStatusCode.NotFound,
                                    "Not found",
                                    "Group not found."
                                )
                            } else {
                                val crashId = call.parameters["crashId"]
                                val crash = crashId?.let {
                                    crashManager.getCrash(
                                        packageName = appParameter.packageName,
                                        crashId = it
                                    )
                                }

                                if (crashId == null || crash == null) {
                                    materialDashboard.errorPage(
                                        call,
                                        HttpStatusCode.NotFound,
                                        "Not found",
                                        "Crash not found."
                                    )
                                } else {
                                    call.respondText(
                                        contentType = ContentType.Text.Html,
                                        status = HttpStatusCode.OK,
                                        provider = {
                                            materialDashboard.crashPage(
                                                packageName = appParameter.packageName,
                                                appName = appParameter.appName,
                                                group = groupParameter.group,
                                                groupCrashes = groupParameter.crashes,
                                                crashId = crashId,
                                                crash = crash
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    route("obfuscation") {

                        get {
                            val appParameter = call.appParameter()

                            if (appParameter == null) {
                                materialDashboard.errorPage(
                                    call,
                                    HttpStatusCode.NotFound,
                                    "Not found",
                                    "App not found."
                                )
                            } else {
                                call.respondText(
                                    contentType = ContentType.Text.Html,
                                    status = HttpStatusCode.OK,
                                    provider = {
                                        materialDashboard.appObfuscationPage(
                                            packageName = appParameter.packageName,
                                            appName = appParameter.appName
                                        )
                                    }
                                )
                            }
                        }

                        post {
                            val appParameter = call.appParameter()

                            if (appParameter == null) {
                                materialDashboard.errorPage(
                                    call,
                                    HttpStatusCode.NotFound,
                                    "Not found",
                                    "App not found."
                                )
                            } else {
                                val parameter = call.receiveMultipart()
                                var versionCodeParameter: PartData.FormItem? = null
                                var mappingFileParameter: PartData.FileItem? = null
                                parameter.forEachPart { part ->
                                    when (part) {
                                        is PartData.FormItem -> {
                                            if (part.name == "versionCode") {
                                                versionCodeParameter = part
                                            }
                                        }
                                        is PartData.FileItem -> {
                                            mappingFileParameter = part
                                        }
                                        is PartData.BinaryItem -> {
                                        }
                                    }
                                }

                                if (versionCodeParameter == null || mappingFileParameter == null) {
                                    call.respond(
                                        status = HttpStatusCode.BadRequest,
                                        message = "versionCode or mappingFile not present"
                                    )
                                } else {
                                    val versionCode = versionCodeParameter?.value?.toLongOrNull()

                                    val mappingFileContents =
                                        mappingFileParameter?.provider?.invoke()?.readText()


                                    if (versionCode == null || mappingFileContents == null) {
                                        call.respond(
                                            status = HttpStatusCode.BadRequest,
                                            message = "versionCode or mappingFile invalid"
                                        )
                                    } else {
                                        obfuscationManager.addMappingFile(
                                            packageName = appParameter.packageName,
                                            appVersionCode = versionCode,
                                            mappingFileContents = mappingFileContents
                                        )

                                        call.respondText(
                                            contentType = ContentType.Text.Html,
                                            status = HttpStatusCode.OK,
                                            provider = {
                                                materialDashboard.appObfuscationPage(
                                                    packageName = appParameter.packageName,
                                                    appName = appParameter.appName,
                                                    uploadSuccess = true
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                route("static") {
                    install(CachingHeaders) {
                        options {
                            CachingOptions(
                                CacheControl.MaxAge(
                                    maxAgeSeconds = TimeUnit.DAYS.toSeconds(31).toInt(),
                                    proxyMaxAgeSeconds = TimeUnit.DAYS.toSeconds(31).toInt(),
                                    mustRevalidate = false,
                                    proxyRevalidate = true,
                                    visibility = CacheControl.Visibility.Public
                                )
                            )
                        }
                    }
                    install(ConditionalHeaders)

                    static {
                        resource(
                            remotePath = "materialize.min.css",
                            resource = "materialize.min.css"
                        )
                        resource(
                            remotePath = "materialize.min.js",
                            resource = "materialize.min.js"
                        )
                        resource(
                            remotePath = "jquery-2.1.1.min.js",
                            resource = "jquery-2.1.1.min.js"
                        )
                        resource(
                            remotePath = "MaterialIcons.woff2",
                            resource = "MaterialIcons.woff2"
                        )
                    }

                    get("init.js") {
                        call.respondText(
                            contentType = ContentType.Application.JavaScript,
                            status = HttpStatusCode.OK,
                            provider = {
                                """
                        (function(${'$'}){
                          ${'$'}(function(){

                            ${'$'}('.sidenav').sidenav();

                          }); // end of document ready
                          
                          ${'$'}(document).ready(function(){
                              ${'$'}('.collapsible').collapsible();
                            });
                            
                              ${'$'}(document).ready(function(){
                                ${'$'}('select').formSelect();
                              });
                        })(jQuery); // end of jQuery name space
                    """.trimIndent()
                            }
                        )
                    }

                    get("MaterialIcons.css") {
                        call.respondText(
                            contentType = ContentType.Text.CSS,
                            status = HttpStatusCode.OK,
                            provider = {
                                """
/* fallback */
@font-face {
  font-family: 'Material Icons';
  font-style: normal;
  font-weight: 400;
  src: url(${environmentVariable.dashboardUrl("static/MaterialIcons.woff2")}) format('woff2');
}

.material-icons {
  font-family: 'Material Icons';
  font-weight: normal;
  font-style: normal;
  font-size: 24px;
  line-height: 1;
  letter-spacing: normal;
  text-transform: none;
  display: inline-block;
  white-space: nowrap;
  word-wrap: normal;
  direction: ltr;
  -webkit-font-feature-settings: 'liga';
  -webkit-font-smoothing: antialiased;
}
                    """.trimIndent()
                            }
                        )
                    }
                }

                dashboardApiRouteManager.install(this)

            }
        }
    }


    private data class AppParameter(
        val packageName: String,
        val appName: String
    )

    private data class GroupParameter(
        val groupId: String,
        val group: CrashManager.CrashGroup,
        val crashes: List<CrashManager.CrashDataItem>
    )


    private suspend fun ApplicationCall.appParameter(): AppParameter? {
        val packageName = this.parameters["packageName"]

        return if (packageName != null) {
            val appName = appStorage.getAppNameForPackageName(packageName)
            if (appName != null) {
                AppParameter(
                    packageName = packageName,
                    appName = appName
                )
            } else {
                null
            }
        } else {
            null
        }
    }


    private suspend fun ApplicationCall.groupParameter(packageName: String): GroupParameter? {
        val groupId = this.parameters["groupId"]
        return if (groupId != null) {
            val pair = crashManager.getCrashesInCrashGroup(
                packageName = packageName,
                groupId = groupId
            )
            if (pair != null) {
                GroupParameter(
                    groupId = groupId,
                    group = pair.first,
                    crashes = pair.second
                )
            } else {
                null
            }
        } else {
            null
        }
    }


}