package dev.moetz.koarl.backend.persistence

import dev.moetz.koarl.api.model.ApiAppData
import kotlinx.serialization.Serializable

interface AppStorage {

    @Serializable
    data class App(
        val packageName: String,
        val appName: String
    )

    suspend fun getStoredApps(): List<App>

    suspend fun getStoredVersionsForPackageName(packageName: String): List<ApiAppData>

    suspend fun getAppNameForPackageName(packageName: String): String?

}