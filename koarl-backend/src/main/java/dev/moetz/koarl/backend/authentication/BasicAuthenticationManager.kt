package dev.moetz.koarl.backend.authentication

import dev.moetz.koarl.backend.environment.EnvironmentVariable
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.routing.Route

class BasicAuthenticationManager(
    private val environmentVariable: EnvironmentVariable
) {

    sealed class Realm(
        val name: String
    ) {
        abstract val userEnvironmentVariable: EnvironmentVariable.Key
        abstract val passwordEnvironmentVariable: EnvironmentVariable.Key

        fun isEnabled(environmentVariable: EnvironmentVariable): Boolean {
            return environmentVariable[userEnvironmentVariable] != null || environmentVariable[passwordEnvironmentVariable] != null
        }

        object GrafanaApi : Realm(name = "Grafana API Access") {
            override val userEnvironmentVariable: EnvironmentVariable.Key
                get() = EnvironmentVariable.Key.Grafana.BasicAuth.User
            override val passwordEnvironmentVariable: EnvironmentVariable.Key
                get() = EnvironmentVariable.Key.Grafana.BasicAuth.Password
        }

        object Dashboard : Realm(name = "Dashboard Access") {
            override val userEnvironmentVariable: EnvironmentVariable.Key
                get() = EnvironmentVariable.Key.Dashboard.BasicAuth.User
            override val passwordEnvironmentVariable: EnvironmentVariable.Key
                get() = EnvironmentVariable.Key.Dashboard.BasicAuth.Password
        }

        object Swagger : Realm(name = "Swagger Access") {
            override val userEnvironmentVariable: EnvironmentVariable.Key
                get() = EnvironmentVariable.Key.Swagger.BasicAuth.User
            override val passwordEnvironmentVariable: EnvironmentVariable.Key
                get() = EnvironmentVariable.Key.Swagger.BasicAuth.Password
        }
    }

    private fun Authentication.Configuration.installBasicAuth(realm: Realm) {
        basic(name = realm.name) {
            this@basic.realm = realm.name
            validate { credentials ->
                if (realm.isEnabled(environmentVariable).not()) {
                    UserIdPrincipal("NoBasicAuthConfigured")
                } else if (credentials.name == environmentVariable[realm.userEnvironmentVariable] && credentials.password == environmentVariable[realm.passwordEnvironmentVariable]) {
                    UserIdPrincipal(credentials.name)
                } else {
                    println("Invalid credentials for $realm. Access denied.")
                    null
                }
            }
        }
    }

    fun install(authenticationConfiguration: Authentication.Configuration) {
        authenticationConfiguration.installBasicAuth(realm = Realm.GrafanaApi)
        authenticationConfiguration.installBasicAuth(realm = Realm.Dashboard)
        authenticationConfiguration.installBasicAuth(realm = Realm.Swagger)
    }

    fun basicAuthentication(route: Route, realm: Realm, build: Route.() -> Unit) {
        if (realm.isEnabled(environmentVariable)) {
            route.authenticate(realm.name) { build.invoke(this) }
        } else {
            build.invoke(route)
        }
    }

}