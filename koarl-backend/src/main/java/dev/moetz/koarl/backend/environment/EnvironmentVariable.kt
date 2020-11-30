package dev.moetz.koarl.backend.environment


/**
 * Wrapper class over environment variables.
 *
 * Internally uses [System.getenv] to retrieve the system's environment variables.
 */
class EnvironmentVariable {

    /**
     * Fetches a system's environment variable set under [key], or null if not set.
     *
     * @param key The [Key] of the Environment-Variable, which should be one of the values defined
     * in this class (inheriting the [Key] interface).
     *
     * @return The value set as environment variable as [key], or `null` if not set.
     */
    operator fun get(key: Key): String? = System.getenv(key.variable) ?: key.default

    /**
     * Fetches a system's environment variable set under [key], or throws an
     * [KotlinNullPointerException] if not set.
     *
     * @param key The [Key] of the Environment-Variable, which should be one of the values defined
     * in this class (inheriting the [Key] interface).
     *
     * @return The value set as environment variable as [key], or `null` if not set.
     * @throws KotlinNullPointerException if no environment variable as [key] is stored.
     */
    @Throws(KotlinNullPointerException::class)
    fun require(key: Key): String {
        return (get(key) ?: key.default)
            ?: throw KotlinNullPointerException("EnvironmentVariable '$key' is required.")
    }

    fun getBoolean(key: Key): Boolean? = get(key)?.toBoolean()

    operator fun contains(key: Key): Boolean = System.getenv().containsKey(key.variable)


    /**
     * interface which types a key to get an environment-variable.
     */
    interface Key {

        val variable: String

        val default: String?

        interface Database {

            /**
             * An enum-class which encapsulates the [EnvironmentVariable.Key]s for SQL database connection.
             */
            enum class Sql(
                override val variable: String,
                override val default: String? = null
            ) : Key {
                Type(variable = "database.type"),
                Host(variable = "database.host"),
                Port(variable = "database.port"),
                DatabaseName(variable = "database.database_name"),
                User(variable = "database.user"),
                Password(variable = "database.password")
            }

        }

        object Grafana {
            /**
             * An enum-class which encapsulates the [EnvironmentVariable.Key]s for the
             * _Grafana_ API `BasicAuth` authentication.
             */
            enum class BasicAuth(
                override val variable: String,
                override val default: String?
            ) : Key {
                User(variable = "grafana.basicauth.user", default = "default_grafana_user"),
                Password(
                    variable = "grafana.basicauth.password",
                    default = "default_grafana_password"
                )
            }
        }

        enum class Application(
            override val variable: String,
            override val default: String?
        ) : Key {
            Port(variable = "application.port", default = "8080"),
            Address(variable = "application.address", default = "0.0.0.0"),

            PublicUrl(variable = "application.url", default = "/");

            companion object {
                fun appendToUrl(environmentVariable: EnvironmentVariable, path: String): String {
                    return environmentVariable.require(PublicUrl).let {
                        if (it.endsWith("/")) {
                            it + path
                        } else {
                            "$it/$path"
                        }
                    }
                }
            }
        }


        object Dashboard {
            /**
             * An enum-class which encapsulates the [EnvironmentVariable.Key]s for the
             * Dashboard `BasicAuth` authentication.
             */
            enum class BasicAuth(
                override val variable: String,
                override val default: String?
            ) : Key {
                User(variable = "dashboard.basicauth.user", default = null),
                Password(variable = "dashboard.basicauth.password", default = null)
            }
        }


        enum class Swagger(
            override val variable: String,
            override val default: String?
        ) : Key {
            Enable(variable = "swagger.enable", default = "true");

            /**
             * An enum-class which encapsulates the [EnvironmentVariable.Key]s for the
             * Swagger UI `BasicAuth` authentication.
             */
            enum class BasicAuth(
                override val variable: String,
                override val default: String?
            ) : Key {
                User(variable = "swagger.basicauth.user", default = null),
                Password(variable = "swagger.basicauth.password", default = null)
            }

        }

    }

}

fun EnvironmentVariable.dashboardUrl(path: String): String {
    return EnvironmentVariable.Key.Application.appendToUrl(
        this,
        "dashboard/$path"
    )
}