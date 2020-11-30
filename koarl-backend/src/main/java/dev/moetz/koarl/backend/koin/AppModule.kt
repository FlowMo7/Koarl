package dev.moetz.koarl.backend.koin

import dev.moetz.koarl.backend.authentication.BasicAuthenticationManager
import dev.moetz.koarl.backend.dashboard.DashboardManager
import dev.moetz.koarl.backend.dashboard.MaterialDashboard
import dev.moetz.koarl.backend.dashboard.api.DashboardApi
import dev.moetz.koarl.backend.dashboard.api.DashboardApiRouteManager
import dev.moetz.koarl.backend.environment.EnvironmentVariable
import dev.moetz.koarl.backend.manager.CrashManager
import dev.moetz.koarl.backend.manager.CrashManagerImpl
import dev.moetz.koarl.backend.obfuscation.ObfuscationManager
import dev.moetz.koarl.backend.obfuscation.StackTraceStringifier
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val appModule = module {

    single<CrashManager> {
        CrashManagerImpl(
            crashStorage = get(),
            obfuscationManager = get()
        )
    }

    single { EnvironmentVariable() }

    single<Json> {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
        }
    }

    single<MaterialDashboard> {
        MaterialDashboard(
            crashManager = get(),
            appStorage = get(),
            stackTraceStringifier = get(),
            obfuscationManager = get(),
            environmentVariable = get()
        )
    }

    single {
        BasicAuthenticationManager(
            environmentVariable = get()
        )
    }

    single {
        DashboardApiRouteManager(
            dashboardApi = get()
        )
    }

    single {
        DashboardManager(
            crashManager = get(),
            appStorage = get(),
            basicAuthenticationManager = get(),
            dashboardApiRouteManager = get(),
            materialDashboard = get(),
            obfuscationManager = get(),
            environmentVariable = get()
        )
    }

    single {
        DashboardApi(
            appStorage = get(),
            crashStorage = get(),
            obfuscationManager = get()
        )
    }


    single<ObfuscationManager> {
        ObfuscationManager(
            obfuscationMappingStorage = get(),
            stackTraceStringifier = get(),
            crashStorage = get()
        )
    }

    single<StackTraceStringifier> {
        StackTraceStringifier()
    }

}