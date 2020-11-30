package dev.moetz.koarl.backend.dashboard

import dev.moetz.koarl.backend.dashboard.api.DashboardApi
import dev.moetz.koarl.backend.dashboard.page.PageManager
import dev.moetz.koarl.backend.environment.EnvironmentVariable
import dev.moetz.koarl.backend.environment.dashboardUrl
import dev.moetz.koarl.backend.manager.CrashManager
import dev.moetz.koarl.backend.obfuscation.ObfuscationManager
import dev.moetz.koarl.backend.obfuscation.StackTraceStringifier
import dev.moetz.koarl.backend.persistence.AppStorage
import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import kotlin.math.ceil

class MaterialDashboard(
    private val crashManager: CrashManager,
    private val appStorage: AppStorage,
    private val stackTraceStringifier: StackTraceStringifier,
    private val obfuscationManager: ObfuscationManager,
    environmentVariable: EnvironmentVariable
) {

    private val pageManager: PageManager =
        PageManager(environmentVariable = environmentVariable)

    internal suspend fun mainPage(): String {
        return pageManager.build("Koarl Dashboard") {

            defaultNavigation(null)

            html {
                h1(
                    text = "Koarl Dashboard",
                    classes = listOf("header", "center", "orange-text")
                )

                div(classes = listOf("container")) {
                    div(classes = listOf("row")) {
                        p("Koarl is a Crash-Reporting Tool for Android Applications. Here you can see the crashes / errors reported for your application.")
                    }
                }


                div(classes = listOf("container")) {
                    div(classes = listOf("row")) {
                        h5(
                            text = "Show crashes for:",
                            classes = listOf("header", "col", "s12", "light")
                        )

                        linkedCollection(
                            items = appStorage.getStoredApps().map { app ->
                                PageManager.HtmlContent.LinkedCollectionItem(
                                    href = environmentVariable.dashboardUrl("app/${app.packageName}"),
                                    text = app.appName
                                )
                            }
                        )
                    }
                }
            }
            footer(null)
        }
    }

    internal suspend fun appPage(
        packageName: String,
        appName: String,
        page: Int = 1,
        limit: Int? = null,
        crashTypes: DashboardApi.CrashType = DashboardApi.CrashType.All
    ): String {
        val realLimit = limit ?: 30

        val allCrashGroups = crashManager.getCrashGroupsForPackageName(packageName = packageName)
            .let { all ->
                when (crashTypes) {
                    DashboardApi.CrashType.All -> all
                    DashboardApi.CrashType.OnlyFatal -> all.filter { it.isFatal }
                    DashboardApi.CrashType.OnlyNonFatal -> all.filterNot { it.isFatal }
                }
            }
            .sortedByDescending { it.numberOfCrashes }

        return pageManager.build("$appName | Koarl Dashboard") {
            defaultNavigation(packageName)

            html {
                h1(
                    text = "Errors in $appName",
                    classes = listOf("header", "center", "orange-text")
                )

                div(classes = listOf("container")) {
                    div(classes = listOf("row")) {
                        span(classes = emptyList(), text = "There are currently&nbsp;")
                        span(classes = listOf("orange-text"), text = "${allCrashGroups.size}")
                        span(
                            classes = emptyList(),
                            text = "&nbsp;different crashes / errors reported, which sum up to&nbsp;"
                        )
                        span(
                            classes = listOf("orange-text"),
                            text = allCrashGroups.sumByDouble { it.numberOfCrashes.toDouble() }
                                .toLong().toString()
                        )
                        span(classes = emptyList(), text = "&nbsp;error / crash events.")
                    }

                    div(classes = listOf("row")) {
                        div(classes = listOf("right")) {
                            div(classes = listOf("col", "s12")) {
                                if(crashTypes != DashboardApi.CrashType.All) {
                                    flatButton(
                                        href = environmentVariable.dashboardUrl("app/$packageName"),
                                        text = "All",
                                        enabled = crashTypes != DashboardApi.CrashType.All
                                    )
                                }
                                flatButton(
                                    href = environmentVariable.dashboardUrl("app/$packageName?type=fatal"),
                                    text = "Fatal Crashes",
                                    enabled = crashTypes != DashboardApi.CrashType.OnlyFatal
                                )
                                flatButton(
                                    href = environmentVariable.dashboardUrl("app/$packageName?type=nonfatal"),
                                    text = "Non-Fatal Errors",
                                    enabled = crashTypes != DashboardApi.CrashType.OnlyNonFatal
                                )
                            }
                        }
                    }

                    div(classes = listOf("row")) {

                        val crashGroups = allCrashGroups
                            .asSequence()
                            .drop((page - 1) * realLimit)
                            .take(realLimit)
                            .toList()

                        table(
                            classes = listOf(
                                "col",
                                "s12",
                                "highlight"
                            ),
                            headers = listOf(
                                "Error Cause",
                                "Number of occurrences"
                            ),
                            items = crashGroups,
                            itemToHtml = { colIndex, group ->
                                when (colIndex) {
                                    0 -> {
                                        a(
                                            classes = emptyList(),
                                            href = environmentVariable.dashboardUrl("app/$packageName/group/${group.uuid}"),
                                            text = "${group.similarities.name}: ${group.similarities.message}"
                                        )
                                    }
                                    1 -> {
                                        val (caption, color) = if (group.isFatal) {
                                            Pair("fatal", "red")
                                        } else {
                                            Pair("non-fatal", "green")
                                        }

                                        if (group.numberOfCrashes == 1L) {
                                            newBadge(
                                                caption = caption,
                                                text = group.numberOfCrashes.toString(),
                                                color = color
                                            )
                                        } else {
                                            val numberOfCrashes = group.numberOfCrashes
                                            val numberOfCrashesString = when {
                                                numberOfCrashes > 1_000_000L -> "${(numberOfCrashes / 1_000_000.0)} M"
                                                numberOfCrashes > 1_000L -> "${(numberOfCrashes / 1_000.0)} k"
                                                else -> group.numberOfCrashes.toString()
                                            }
                                            newBadge(
                                                caption = caption,
                                                text = numberOfCrashesString,
                                                color = color
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        pagination(
                            numberOfPages = ceil(allCrashGroups.size / realLimit.toFloat()).toInt(),
                            activePage = page,
                            maxDisplayedPages = 20,
                            linkBuilder = { page: Int ->
                                environmentVariable.dashboardUrl("app/$packageName?page=$page")
                            }
                        )
                    }


                    div(classes = listOf("row")) {
                        div(classes = listOf("left")) {
                            text("If you are using&nbsp;")
                            a(
                                classes = emptyList(),
                                href = "https://developer.android.com/studio/build/shrink-code#configuration-files",
                                text = "ProGuard / R8"
                            )
                            text(", which you should, consider uploading the respective&nbsp;")
                            a(
                                classes = emptyList(),
                                href = "https://developer.android.com/studio/build/shrink-code#decode-stack-trace",
                                text = "mapping.txt"
                            )
                            text("&nbsp;here, to enable Koarl decoding an obfuscated stack trace.")
                        }

                        div(classes = listOf("right")) {
                            button(
                                href = environmentVariable.dashboardUrl("app/$packageName/obfuscation"),
                                text = "Mapping files"
                            )
                        }
                    }
                }
            }
            footer("app/$packageName/group")
        }
    }

    internal suspend fun groupPage(
        packageName: String,
        appName: String,
        crashes: List<CrashManager.CrashDataItem>,
        group: CrashManager.CrashGroup,
        currentPage: Int = 1
    ): String {
        return pageManager.build("Group ${group.similarities.name}: ${group.similarities.message} | $appName | Koarl Dashboard") {
            defaultNavigation(packageName)

            html {
                h1(
                    text = appName,
                    classes = listOf(
                        "header",
                        "center",
                        "orange-text"
                    )
                )
                div(classes = listOf("container")) {
                    h5(
                        text = "Stack-Trace:",
                        classes = listOf(
                            "header",
                            "col",
                            "s12",
                            "light"
                        )
                    )
                    div(classes = listOf("row")) {
                        pre(
                            classes = listOf("s12", "light"),
                            text = stackTraceStringifier.getStackTraceString(
                                group.similarities
                            )
                        )
                    }

                    div(classes = listOf("row")) {
                        h5(
                            text = "Sessions / Crashes:",
                            classes = listOf(
                                "header",
                                "col",
                                "s12",
                                "light"
                            )
                        )

                        val limit = 10

                        val pagedList = crashes
                            .asSequence()
                            .drop(limit * (currentPage - 1))
                            .take(limit)
                            .toList()

                        pagination(
                            numberOfPages = ceil(crashes.size.toFloat() / limit).toInt(),
                            activePage = currentPage,
                            maxDisplayedPages = 20,
                            linkBuilder = { page: Int ->
                                environmentVariable.dashboardUrl("app/$packageName/group/${group.uuid}?page=$page")
                            }
                        )

                        table(
                            classes = listOf(
                                "col",
                                "s12",
                                "highlight"
                            ),
                            headers = listOf(
                                "Timestamp",
                                "OS version",
                                "Device name",
                                "Device brand",
                                "App version"
                            ),
                            items = pagedList,
                            itemToHtml = { index, crash ->
                                when (index) {
                                    0 -> a(
                                        classes = emptyList(),
                                        href = environmentVariable.dashboardUrl("app/$packageName/group/${group.uuid}/crash/${crash.crash.uuid}"),
                                        text = "${crash.crash.dateTime}"
                                    )
                                    1 -> span(
                                        classes = emptyList(),
                                        text = "${crash.deviceData?.operationSystemVersion}"
                                    )
                                    2 -> span(
                                        classes = emptyList(),
                                        text = "${crash.deviceData?.deviceName}"
                                    )
                                    3 -> span(
                                        classes = emptyList(),
                                        text = "${crash.deviceData?.brand}"
                                    )
                                    4 -> span(
                                        classes = emptyList(),
                                        text = "${crash.appData.appVersionName} (${crash.appData.appVersionCode})"
                                    )
                                }
                            }
                        )
                    }
                }
            }
            footer("app/$packageName/group/${group.uuid}/crash")
        }
    }


    internal suspend fun crashPage(
        packageName: String,
        appName: String,
        group: CrashManager.CrashGroup,
        groupCrashes: List<CrashManager.CrashDataItem>,
        crashId: String,
        crash: CrashManager.CrashDataItem
    ): String {
        return pageManager.build("${crash.crash.dateTime} | Group ${group.similarities.name}: ${group.similarities.message} | $appName | Koarl Dashboard") {
            defaultNavigation(packageName)
            try {

                html {
                    h1(
                        text = appName,
                        classes = listOf(
                            "header",
                            "center",
                            "orange-text"
                        )
                    )

                    div(classes = listOf("container")) {

                        pagination(
                            numberOfPages = groupCrashes.size,
                            activePage = groupCrashes.indexOfFirst { it.crash.uuid == crashId } + 1,
                            maxDisplayedPages = 20,
                            linkBuilder = { page: Int ->
                                environmentVariable.dashboardUrl("app/$packageName/group/${group.uuid}/crash/${groupCrashes[page - 1].crash.uuid}")
                            }
                        )

                        h5(
                            text = "Crash:",
                            classes = listOf(
                                "header",
                                "col",
                                "s12",
                                "light"
                            )
                        )
                        div(classes = listOf("row")) {
                            val items = listOf<Pair<String, String>>(
                                "uuid" to crash.crash.uuid,
                                "isFatal" to crash.crash.isFatal.toString(),
                                "inForeground" to crash.crash.inForeground.toString(),
                                "dateTime" to crash.crash.dateTime.toString()
                            )
                            table(
                                classes = emptyList(),
                                headers = listOf(
                                    "Key",
                                    "Value"
                                ),
                                items = items
                            ) { index, (key, value) ->
                                when (index) {
                                    0 -> span(
                                        classes = emptyList(),
                                        text = key
                                    )
                                    1 -> span(
                                        classes = emptyList(),
                                        text = value
                                    )
                                }
                            }

                        }


                        h5(
                            text = "Stack-Trace:",
                            classes = listOf(
                                "header",
                                "col",
                                "s12",
                                "light"
                            )
                        )
                        div(classes = listOf("row")) {
                            pre(
                                classes = listOf("s12", "light"),
                                text = stackTraceStringifier.getStackTraceString(
                                    crash.crash.throwable
                                )
                            )
                        }

                        h5(
                            text = "Device Data:",
                            classes = listOf(
                                "header",
                                "col",
                                "s12",
                                "light"
                            )
                        )
                        div(classes = listOf("row")) {
                            if (crash.deviceData != null) {
                                val items = listOf<Pair<String, String>>(
                                    "deviceName" to crash.deviceData.deviceName,
                                    "manufacturer" to crash.deviceData.manufacturer,
                                    "brand" to crash.deviceData.brand,
                                    "model" to crash.deviceData.model,
                                    "buildId" to crash.deviceData.buildId,
                                    "operationSystemVersion" to crash.deviceData.operationSystemVersion.toString()
                                )
                                table(
                                    classes = emptyList(),
                                    headers = listOf(
                                        "Key",
                                        "Value"
                                    ),
                                    items = items
                                ) { index, (key, value) ->
                                    when (index) {
                                        0 -> span(
                                            classes = emptyList(),
                                            text = key
                                        )
                                        1 -> span(
                                            classes = emptyList(),
                                            text = value
                                        )
                                    }
                                }

                            } else {
                                //TODO no device data view / text
                                cardPanel(color = "teal") {
                                    span(
                                        classes = listOf("white-text"),
                                        text = "DeviceData not available."
                                    )
                                }
                            }
                        }


                        h5(
                            text = "App Data:",
                            classes = listOf(
                                "header",
                                "col",
                                "s12",
                                "light"
                            )
                        )
                        div(classes = listOf("row")) {
                            val items = listOf<Pair<String, String>>(
                                "packageName" to crash.appData.packageName,
                                "appName" to crash.appData.appName,
                                "appVersionCode" to crash.appData.appVersionCode.toString(),
                                "appVersionName" to crash.appData.appVersionName
                            )
                            table(
                                classes = emptyList(),
                                headers = listOf(
                                    "Key",
                                    "Value"
                                ),
                                items = items
                            ) { index, (key, value) ->
                                when (index) {
                                    0 -> span(
                                        classes = emptyList(),
                                        text = key
                                    )
                                    1 -> span(
                                        classes = emptyList(),
                                        text = value
                                    )
                                }
                            }

                        }

                    }
                }

            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }

            footer("app/$packageName/group/${group.uuid}/crash/$crashId")
        }
    }


    internal suspend fun appObfuscationPage(
        packageName: String,
        appName: String,
        uploadSuccess: Boolean? = null
    ): String {
        return pageManager.build("$appName | Koarl Dashboard") {
            defaultNavigation(packageName)

            html {
                h1(
                    text = appName,
                    classes = listOf("header", "center", "orange-text")
                )

                div(classes = listOf("container")) {

                    if (uploadSuccess != null) {
                        div(classes = listOf("row")) {
                            div(classes = listOf("col", "s8", "center")) {
                                if (uploadSuccess) {
                                    cardPanel("teal") {
                                        span(
                                            classes = listOf("white-text"),
                                            text = "Upload succeeded"
                                        )
                                    }
                                } else {
                                    cardPanel("red accent-4") {
                                        span(classes = listOf("white-text"), text = "Upload failed")
                                    }
                                }
                            }
                        }
                    }

                    div(classes = listOf("row")) {
                        div(classes = listOf("col", "s12")) {
                            text("If you are using&nbsp;")
                            a(
                                classes = emptyList(),
                                href = "https://developer.android.com/studio/build/shrink-code#configuration-files",
                                text = "ProGuard / R8"
                            )
                            text(", which you should, consider uploading the respective&nbsp;")
                            a(
                                classes = emptyList(),
                                href = "https://developer.android.com/studio/build/shrink-code#decode-stack-trace",
                                text = "mapping.txt"
                            )
                            text("&nbsp;here, to enable Koarl decoding an obfuscated stack trace.")
                            breakLine()
                            text("For each new build / version you create, a new mapping.txt file is created. For each version you want Koarl to be able to decode obfuscated stack traces, the respective mapping.txt file needs to be added here.")
                        }
                    }


                    div(classes = listOf("row")) {
                        h5(
                            text = "Uploaded obfuscation mappings",
                            classes = listOf("header", "col", "s12", "light")
                        )
                    }

                    val obfuscationMappingVersionCodes =
                        obfuscationManager.getObfuscationMappingVersionCodes(packageName = packageName)


                    if (obfuscationMappingVersionCodes.isNotEmpty()) {
                        div(classes = listOf("row")) {
                            table(
                                classes = emptyList(),
                                headers = listOf(
                                    "VersionCode",
                                    "Zum Mapping"
                                ),
                                items = obfuscationMappingVersionCodes
                            ) { index, versionCode ->
                                when (index) {
                                    0 -> span(
                                        classes = emptyList(),
                                        text = versionCode.toString()
                                    )
                                    1 -> a(
                                        classes = emptyList(),
                                        href = environmentVariable.dashboardUrl("app/$packageName/obfuscation/$versionCode"),
                                        text = "Mapping"
                                    )
                                }
                            }
                        }
                    } else {
                        div(classes = listOf("row")) {
                            div(classes = listOf("s6", "center")) {
                                cardPanel("orange lighten-2") {
                                    span(emptyList(), "No mapping file found.")
                                }
                            }
                        }
                    }

                    form(
                        action = environmentVariable.dashboardUrl("app/$packageName/obfuscation"),
                        method = "post"
                    ) {
                        div(classes = listOf("row")) {
                            h5(
                                text = "Upload Mapping",
                                classes = listOf("header", "col", "s12", "light")
                            )

                            div(classes = listOf("col", "s10")) {
                                fileUpload("Select Mapping file", "mappingFile")
                            }

                            textInput(
                                classes = listOf("col", "s2"),
                                placeholder = "17",
                                id = "versionCode",
                                type = "number",
                                hint = "Version Code"
                            )
                        }

                        div(classes = listOf("row")) {
                            div(classes = listOf("col", "s2", "right")) {
                                submitButton(
                                    name = "action",
                                    label = "Upload"
                                )
                            }
                        }
                    }
                }
            }

            footer("app/$packageName/obfuscation")
        }
    }


    private suspend fun PageManager.PageBuilder.defaultNavigation(activePackageName: String?) {
        navigation(
            logoName = "Koarl",
            links = appStorage.getStoredApps().map { app ->
                PageManager.PageBuilder.NavigationLink(
                    name = app.appName,
                    href = environmentVariable.dashboardUrl("app/${app.packageName}"),
                    isActive = app.packageName == activePackageName
                )
            }
        )
    }

    internal suspend fun errorPage(
        call: ApplicationCall,
        httpStatusCode: HttpStatusCode,
        title: String,
        message: String
    ) {
        call.respondText(
            contentType = ContentType.Text.Html,
            status = httpStatusCode,
            provider = {
                pageManager.build("$title | Koarl Dashboard") {
                    defaultNavigation(null)
                    html {
                        h1(
                            text = title,
                            classes = listOf("header", "center", "orange-text")
                        )

                        span(
                            classes = listOf("center", "s12"),
                            text = message
                        )
                    }
                    footer(null)
                }
            }
        )
    }
}



