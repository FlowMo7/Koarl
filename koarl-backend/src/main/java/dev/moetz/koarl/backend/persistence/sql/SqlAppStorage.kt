package dev.moetz.koarl.backend.persistence.sql

import dev.moetz.koarl.api.model.ApiAppData
import dev.moetz.koarl.backend.persistence.AppStorage
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class SqlAppStorage(
    private val database: Database
) : AppStorage {

    override suspend fun getStoredApps(): List<AppStorage.App> {
        return suspendedTransaction(database) {
            AppDataTable
                .slice(AppDataTable.packageName, AppDataTable.appName)
                .selectAll()
                .withDistinct()
                .map { resultRow ->
                    AppStorage.App(
                        packageName = resultRow[AppDataTable.packageName],
                        appName = resultRow[AppDataTable.appName]
                    )
                }
        }
    }

    override suspend fun getStoredVersionsForPackageName(packageName: String): List<ApiAppData> {
        return suspendedTransaction(database) {
            AppDataTable
                .slice(
                    AppDataTable.packageName,
                    AppDataTable.appName,
                    AppDataTable.appVersionCode,
                    AppDataTable.appVersionName
                )
                .selectAll()
                .withDistinct()
                .map { resultRow ->
                    ApiAppData(
                        packageName = resultRow[AppDataTable.packageName],
                        appName = resultRow[AppDataTable.appName],
                        appVersionCode = resultRow[AppDataTable.appVersionCode],
                        appVersionName = resultRow[AppDataTable.appVersionName]
                    )
                }
        }
    }

    override suspend fun getAppNameForPackageName(packageName: String): String? {
        return suspendedTransaction(database) {
            AppDataTable
                .slice(AppDataTable.appName)
                .select { AppDataTable.packageName eq packageName }
                .orderBy(AppDataTable.id, SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.let { resultRow -> resultRow[AppDataTable.appName] }
        }
    }

}