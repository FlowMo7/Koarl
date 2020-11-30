package dev.moetz.koarl.backend.grafana

import dev.moetz.koarl.backend.authentication.BasicAuthenticationManager
import io.ktor.routing.Route
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module


/**
 * A [Module] which includes all instances for _Grafana_.
 */
val grafanaModule: Module = module {

    single {
        GrafanaApi(
            json = Json {
                useArrayPolymorphism = true
                ignoreUnknownKeys = true
            }
        )
    }

    single {
        GrafanaManager(
            grafanaApi = get(),
            grafanaDataSource = get(),
            basicAuthenticationManager = get()
        )
    }

    single {
        GrafanaDataSource(
            crashStorage = get(),
            appStorage = get(),
            grafanaApi = get()
        )
    }
}


/**
 * A Manager which wraps the [GrafanaApi] and [GrafanaDataSource] together.
 *
 * This eases the use, as just this class needs to be exposed in e.g. Koin, as only the
 * [installRoute] method needs to be invoked, as the data-source from [GrafanaDataSource.init] is
 * called in the init method of this class.
 *
 * @param grafanaApi The [GrafanaApi] to expose when calling [installRoute]
 * @param [grafanaDataSource] The [GrafanaDataSource] to call [GrafanaDataSource.init] on.
 */
class GrafanaManager(
    private val grafanaApi: GrafanaApi,
    private val grafanaDataSource: GrafanaDataSource,
    private val basicAuthenticationManager: BasicAuthenticationManager
) {

    init {
        GlobalScope.launch {
            grafanaDataSource.init()
        }
    }


    /**
     * Installs the _Grafana_ API into the given [route].
     * There is no authentication done inside here, so the passed route should already be wrapped
     * in an authorization.
     *
     * @param route The route to add the API to.
     *
     * @see [GrafanaApi.installRoute]
     */
    fun installRoute(route: Route) {
        basicAuthenticationManager.basicAuthentication(
            route,
            BasicAuthenticationManager.Realm.GrafanaApi
        ) {
            grafanaApi.installRoute(this)
        }
    }

}