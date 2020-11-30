package dev.moetz.koarl.backend.swagger

import dev.moetz.koarl.api.serializer.DateTimeSerializer
import dev.moetz.koarl.backend.dashboard.MaterialDashboard
import dev.moetz.koarl.backend.dashboard.api.DashboardApi
import dev.moetz.swagger.builder.SwaggerBuilder
import dev.moetz.swagger.definition.SwaggerDefinition
import org.joda.time.DateTime
import java.util.*

object SwaggerDefinitionProvider {

    private val version: String by lazy {
        this::class.java.`package`.implementationVersion ?: "undefined"
    }

    private fun SwaggerBuilder.Path.dashboardApiBasicAuth() {
        headerParameter("Authorization") {
            required(false)
            description("Basic Authorization to access the 'grafana-dashboard-api' resources.")
            type("string")
        }
    }

    private fun SwaggerBuilder.Path.grafanaBasicAuth() {
        headerParameter("Authorization") {
            required(true)
            description("Basic Authorization to access the 'grafana-api' resources.")
            type("string")
        }
    }

    private fun SwaggerBuilder.Path.libraryVersionHeader() {
        headerParameter("X-Library-Version") {
            required(true)
            description("The version of the library which performs the request.")
            type("string")
        }
    }

    fun get(): SwaggerDefinition {
        val currentLibraryApiVersionPath = "dev-v1"
        return SwaggerBuilder.generate {
            info {
                title("Koarl API")
                version(version)
                description("API for the Koarl REST API")
                host("koarl.dev")
                basePath("/")
                schemes("https")
            }

            val appDataModel = createObjectSchema("AppData") {
                required(true)
                description("Details about the device the crashes originate from.")

                property("packageName", createTypeSchema("string") {
                    required(true)
                    description("The packageName of the app the crashes are reported from.")
                    example("dev.moetz.koarl.sample")
                })

                property("appName", createTypeSchema("string") {
                    required(true)
                    description("The name of the app the crashes are reported from.")
                    example("Koarl Sample Application")
                })

                property("appVersionCode", createTypeSchema("integer") {
                    required(true)
                    format("int64")
                    description("The version-code of the app the crashes are reported from.")
                    example("237")
                })

                property("appVersionName", createTypeSchema("string") {
                    required(true)
                    description("The version-name of the app the crashes are reported from.")
                    example("1.6.17")
                })
            }

            val deviceDataModel = createObjectSchema("DeviceData") {
                required(true)
                description("Details about the device the crashes originate from.")

                property("deviceName", createTypeSchema("string") {
                    required(true)
                    description("The name of the device.")
                    example("generic_x86")
                })

                property("manufacturer", createTypeSchema("string") {
                    required(true)
                    description("The name of the manufacturer of the device.")
                    example("Google")
                })

                property("brand", createTypeSchema("string") {
                    required(true)
                    description("The name of the brand of the device.")
                    example("google")
                })

                property("model", createTypeSchema("string") {
                    required(true)
                    description("The name of the model of the device.")
                    example("Android SDK built for x86")
                })

                property("buildId", createTypeSchema("string") {
                    required(true)
                    description("The name of the buildId of the device.")
                    example("OSM1.180201.007")
                })

                property("operationSystemVersion", createTypeSchema("integer") {
                    required(true)
                    description("The operationSystemVersion of the device.")
                    example("28")
                })
            }

            val crashModel = createObjectSchema("Crash") {
                required(true)
                description("A (fatal or non-fatal) crash.")

                property("uuid", createTypeSchema("string") {
                    required(true)
                    description("A device-generated random UUID for the given crash.")
                    example(UUID.randomUUID().toString())
                })

                property("isFatal", createTypeSchema("boolean") {
                    required(true)
                    description("Whether this crash was a fatal crash (which shutdown the app) or a non-fatal crash.")
                    example("true")
                })

                property("dateTime", createTypeSchema("string") {
                    required(true)
                    description("The datetime of the crash occurance.")
                    example(DateTimeSerializer.serializeToString(DateTime.now()))
                    format(DateTimeSerializer.formatString)
                })

                property("throwable", createObjectSchema("Throwable") {
                    required(true)
                    description("The throwable of the crash.")

                    property("name", createTypeSchema("string") {
                        required(false)
                        description("The name of the exception.")
                        example("IllegalStateException")
                    })

                    property("message", createTypeSchema("string") {
                        required(false)
                        description("The message of the throwable.")
                        example("Something, somewhere, went terribly wrong")
                    })

                    property("localizedMessage", createTypeSchema("string") {
                        required(false)
                        description("The localizedMessage of the throwable.")
                        example("Something, somewhere, went terribly wrong")
                    })

                    property("stackTrace", createArraySchema {
                        required(true)
                        description("The stacktrace of the throwable.")

                        items(createObjectSchema("StackTraceElement") {
                            required(true)
                            description("The item of the stacktrace of a throwable.")

                            property("fileName", createTypeSchema("string") {
                                required(false)
                                description("The file name of the stack trace entry.")
                                example("MainActivity.kt")
                            })

                            property("lineNumber", createTypeSchema("integer") {
                                required(false)
                                description("The line number of the stack trace entry.")
                                example("17")
                            })

                            property("className", createTypeSchema("string") {
                                required(false)
                                description("The class name of the stack trace entry.")
                                example("MainActivity")
                            })

                            property("methodName", createTypeSchema("string") {
                                required(false)
                                description("The method name of the stack trace entry.")
                                example("onCreate")
                            })

                            property("isNativeMethod", createTypeSchema("boolean") {
                                required(true)
                                description("Whether this stack trace element is within a native method.")
                                example("false")
                            })
                        })
                    })

                    property("cause", referenceRecursively(this))
                })
            }


            tag("library-facing", "API endpoints facing the library usage")


            path("/crash", "post") {
                summary("Upload crashes")
                description("Uploads crashes to backend.")
                operationId("/$currentLibraryApiVersionPath/crash")
                tags("library-facing")
                consumes("application/json")

                libraryVersionHeader()

                bodyParameter("body") {
                    required(true)
                    schema(createObjectSchema("CrashUploadRequestBody") {
                        required(true)

                        property("deviceData", deviceDataModel)

                        property("appData", appDataModel)

                        property("crashes", createArraySchema {
                            required(true)
                            description("A list of crashes to report")
                            items(crashModel)
                        })
                    })
                }

                response(204) {
                    description("Crashes stored successfully")
                }
            }


            tag(
                "dashboard-api",
                "API endpoints which provide access to retrieve the crashes data to show in a dashboard i.e."
            )


            path("/dashboard/api/app", "get") {
                summary("Get a list of all applications which this backend has crashes stored of.")
                description("Get a list of all applications which this backend has crashes stored of.")
                operationId("/dashboard/api/app")
                tags("dashboard-api")
                produces("application/json")

                dashboardApiBasicAuth()

                response(200) {
                    description("Returns a list of all applications (unique package names) which this backend has crashes stored of.")
                    schema(createObjectSchema("DashboardAppListResponse") {
                        description("Wrapper object around the list of applications.")
                        required(true)

                        property("apps", createArraySchema {
                            description("The list of applications")
                            required(true)

                            items(createObjectSchema("DashboardApp") {
                                description("An application of which at least one crash is stored")
                                required(true)

                                property("packageName", createTypeSchema("string") {
                                    description("The packageName of the app.")
                                    required(true)
                                    example("dev.moetz.koarl.sample")
                                })

                                property("appName", createTypeSchema("string") {
                                    description("The nam of the app (as it is visible to the user).")
                                    required()
                                    example("Koarl Sample Application")
                                })
                            })
                        })
                    })
                }
            }

            path("/dashboard/api/app/{packageName}/group", "get") {
                summary("Get a list of 'crash-groups' of the app given by it's packageName.")
                description("Returns a list of 'crash-groups' of the given packageName. A 'crash-group' are multiple crashes which look the same (same exception, message, stacktrace, ...). They can somewhat be seen as a unique representation of crash-causes.")
                operationId("/dashboard/api/app/{packageName}/group")
                tags("dashboard-api")
                produces("application/json")

                dashboardApiBasicAuth()

                pathParameter("packageName", "string") {
                    description("The packageName to get the crash-groups of.")
                    required(true)
                }

                queryParameter("type") {
                    description("The type of error / crashes to return (${DashboardApi.CrashType.values().map { it.key }}). Returns all if not set.")
                    required(false)
                    enum(*DashboardApi.CrashType.values().map { it.key }.toTypedArray())
                }

                response(200) {
                    description("Returns a list of crash-groups.")
                    schema(createObjectSchema("DashboardCrashesForAppsResponse") {
                        description("Wrapper object around the list crash-groups")
                        required(true)

                        property("crashes", createArraySchema {
                            description("The list of crash-groups")
                            required(true)

                            items(createObjectSchema("DashboardCrashGroup") {
                                description("Information about a crash-group and the amount of (similar) crashes which this group contains.")
                                required(true)

                                property("groupId", createTypeSchema("string") {
                                    description("The UUID of the crash-group.")
                                    format("uuid")
                                    required(true)
                                    example("ea000b85-fa9a-49e7-bddb-07b56d69a380")
                                })

                                property(
                                    "throwable",
                                    createObjectSchema("DashboardSneakPeakThrowable") {
                                        description("A somewhat 'sneak peak' to the crashes within this crash-group. This contains the stacktrace as well as information about the throwable, but does not contain any session / device data (which is bound to single crashes, and may differ within a crash-group).")
                                        required(true)

                                        property("name", createTypeSchema("string") {
                                            description("The name of the exception of the crashes within this crash-group.")
                                            required(false)
                                            example("KotlinNullPointerException")
                                        })

                                        property("message", createTypeSchema("string") {
                                            description("The message of the exception of the crashes within this crash-group.")
                                            required(false)
                                            example("The specified value was null.")
                                        })

                                        property("stackTrace", createArraySchema {
                                            description("The list of stacktrace-elements of the exception of the crashes within this crash-group.")
                                            required(true)

                                            items(createObjectSchema {
                                                description("A stack-trace element of an exception.")
                                                required(true)

                                                property("className", createTypeSchema("string") {
                                                    required(false)
                                                    description("The class name of the stack trace entry.")
                                                    example("MainActivity")
                                                })

                                                property("methodName", createTypeSchema("string") {
                                                    required(false)
                                                    description("The method name of the stack trace entry.")
                                                    example("onCreate")
                                                })

                                                property("lineNumber", createTypeSchema("integer") {
                                                    required(false)
                                                    description("The line number of the stack trace entry.")
                                                    example("17")
                                                })

                                                property(
                                                    "isNativeMethod",
                                                    createTypeSchema("boolean") {
                                                        required(true)
                                                        description("Whether this stack trace element is within a native method.")
                                                        example("false")
                                                    })
                                            })
                                        })

                                        property("cause", referenceRecursively(this))
                                    })

                                property("numberOfCrashes", createTypeSchema("number") {
                                    format("double")
                                    description("The number of crashes within this crash-group.")
                                    required(true)
                                    example("17")
                                })
                            })
                        })
                    })
                }

                response(400) {
                    description("The 'packageName' has not been set.")
                }

                response(404) {
                    description("The given 'packageName' has not been found.")
                }

            }

            path("/dashboard/api/app/{packageName}/group/{groupId}/crash", "get") {
                summary("Get the list of crashes within the given crash-group.")
                description("Get the list of crashes within the given crash-group, as well as information about the crash-group.")
                operationId("/dashboard/api/app/{packageName}/group/{groupId}/crash")
                tags("dashboard-api")
                produces("application/json")

                dashboardApiBasicAuth()

                pathParameter("packageName", "string") {
                    description("The packageName to get the crashes of.")
                    required(true)
                }

                pathParameter("groupId", "string") {
                    description("The id of the crash-group to get the crashes of.")
                    required(true)
                }

                response(200) {
                    description("Returns a list of crashes within a crash-group.")
                    schema(createObjectSchema("DashboardCrashListInGroupResponse") {
                        description("Wrapper object around the list crashes within a crash-group.")
                        required(true)


                        property("group", getNamedSchema("DashboardCrashGroup"))

                        property("crashes", createArraySchema {
                            description("The list of crashes within the given crash-group.")
                            required(true)

                            items(createObjectSchema("DashboardCrash") {
                                description("Information about a crash, the devices state at the time of the crash, and information about the app (-version) in which the crash occured.")
                                required(true)

                                property("appData", appDataModel)

                                property("deviceData", deviceDataModel)

                                property("crash", crashModel)

                            })
                        })
                    })
                }

                response(400) {
                    description("The 'packageName' or 'groupId' has not been set.")
                }

                response(404) {
                    description("The given 'packageName' or 'groupId' has not been found.")
                }

            }

            path("/dashboard/api/app/{packageName}/group/{groupId}/crash/{crashId}", "get") {
                summary("Get a crash of a given crash-group for a given application.")
                description("Get a crash of a given crash-group for a given application.")
                operationId("/dashboard/api/app/{packageName}/group/{groupId}/crash/{crashId}")
                tags("dashboard-api")
                produces("application/json")

                dashboardApiBasicAuth()

                pathParameter("packageName", "string") {
                    description("The packageName to get the crash of.")
                    required(true)
                }

                pathParameter("groupId", "string") {
                    description("The id of the crash-group to get the crash of.")
                    required(true)
                }

                pathParameter("crashId", "string") {
                    description("The id of the crash to get.")
                    required(true)
                }

                response(200) {
                    description("Returns a given crash-item for a given packageName in a given crash-group.")
                    schema(createObjectSchema("DashboardCrashInGroupResponse") {
                        description("Wrapper object around the crash within a crash-group.")
                        required(true)


                        property("group", getNamedSchema("DashboardCrashGroup"))

                        property("crash", getNamedSchema("DashboardCrash"))

                    })
                }

                response(400) {
                    description("The 'packageName', 'groupId' or 'crashId' has not been set.")
                }

                response(404) {
                    description("The given 'packageName', 'groupId' or 'crashId' has not been found.")
                }

            }


            path("/dashboard/api/app/{packageName}/obfuscation", "get") {
                summary("Get a list of version-codes for which an obfuscation-mapping is available for the given 'packageName'.")
                description("Get a list of version-codes for which an obfuscation-mapping is available for the given 'packageName'.")
                operationId("/dashboard/api/app/{packageName}/obfuscation")
                tags("dashboard-api")
                produces("application/json")

                dashboardApiBasicAuth()

                pathParameter("packageName", "string") {
                    description("The packageName to get the list of versionCodes of.")
                    required(true)
                }

                response(200) {
                    description("Returns a list of versionCodes.")
                    schema(createObjectSchema("DashboardObfuscationMappingVersionCodes") {
                        description("Wrapper object around the list versionCodes.")
                        required(true)

                        property("versionCodes", createArraySchema {
                            description("The list of versionCodes.")
                            required(true)

                            items(createTypeSchema("integer") {
                                description("The versionCode of an available obfuscation mapping for the given 'packageName'.")
                                required(true)
                                format("int64")
                                example("17")
                            })
                        })
                    })
                }

                response(400) {
                    description("The 'packageName' has not been set.")
                }

            }

            path("/dashboard/api/app/{packageName}/obfuscation/{versionCode}/mapping", "get") {
                summary("Returns the obfuscation mapping for the given.")
                description("Returns the obfuscation mapping for the given 'packageName' and the given 'versionCode', which was uploaded and persisted beforehand here.")
                operationId("/dashboard/api/app/{packageName}/obfuscation/{versionCode}/mapping")
                tags("dashboard-api")
                produces("text/plain")

                dashboardApiBasicAuth()

                pathParameter("packageName", "string") {
                    description("The packageName to get the mapping of.")
                    required(true)
                }

                pathParameter("versionCode", "number") {
                    //TODO format("long")
                    description("The versionCode to get the mapping of.")
                    required(true)
                }

                response(200) {
                    description("The obfuscation mapping for the given 'packageName' and 'versionCode'.")
                }

                response(400) {
                    description("The 'packageName' or 'versionCode' has not been set.")
                }

                response(404) {
                    description("No obfuscation mapping for the given 'packageName' and 'versionCode' found.")
                }

            }

            path("/dashboard/api/app/{packageName}/obfuscation/{versionCode}/mapping", "post") {
                summary("Upload an obfuscation mapping.")
                description("Adds an obfuscation mapping to the given 'packageName' for the given 'versionCode'. Any previously uploaded mapping for the given 'packageName' and 'versionCode' (the combination of those two) will be overwritten (there can always only be one mapping set for the combination of packageName and versionCode).")
                operationId("/dashboard/api/app/{packageName}/obfuscation/{versionCode}/mapping-post")
                tags("dashboard-api")
                consumes("text/plain")

                dashboardApiBasicAuth()

                pathParameter("packageName", "string") {
                    description("The packageName to set the mapping of.")
                    required(true)
                }

                pathParameter("versionCode", "integer") {
                    //TODO format("int64")
                    description("The versionCode to set the mapping of.")
                    required(true)
                }

                bodyParameter("obfuscationMapping") {
                    description("The obfuscation mapping, as generated from e.g. ProGuard or R8 as 'mapping.txt'.")
                    required(true)
//                    type("string")
                }

                response(204) {
                    description("The mapping has successfully been uploaded.")
                }

                response(400) {
                    description("The 'packageName' or 'versionCode' has not been set.")
                }

            }





            tag("grafana-api", "API endpoints acting as Grafana data sources")

            path("/grafana-api", "get") {
                summary("Health-check for Grafana")
                description("Responds 200 if data-source is healthy.")
                operationId("/grafana-api")
                tags("grafana-api")
                produces("application/json")
                grafanaBasicAuth()


                response(200) {
                    description("Grafana data-source is healthy.")
                }
            }

            path("/grafana-api/search", "post") {
                summary("List metric options")
                description("Used by the find metric options on the query tab in panels.")
                operationId("/grafana-api/search")
                tags("grafana-api")
                produces("application/json")
                grafanaBasicAuth()

                response(200) {
                    description("List of metric options this data-source provides")

                    schema(createArraySchema {
                        required(true)
                        description("List of metric options this data-source provides")

                        items(createTypeSchema("string") {
                            required(true)
                            description("the name of a metric option")
                            example("heartbeats")
                        })

                    })

                }
            }

        }
    }


}