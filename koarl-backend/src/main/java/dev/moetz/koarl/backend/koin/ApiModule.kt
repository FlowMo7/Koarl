package dev.moetz.koarl.backend.koin

import dev.moetz.koarl.backend.api.v1.V1Api
import org.koin.dsl.module

val apiModule = module {
    single<V1Api> {
        V1Api(
            json = get(),
            crashManager = get()
        )
    }
}