package dev.moetz.koarl.backend.persistence.sql

import dev.moetz.koarl.backend.persistence.ObfuscationMappingStorage
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class SqlObfuscationMappingStore(private val database: Database) : ObfuscationMappingStorage {

    override suspend fun insertMapping(
        packageName: String,
        appVersionCode: Long,
        mappingFileContents: String
    ) {
        return suspendedTransaction(database) {
            ObfuscationMappingTable.insert {
                it[ObfuscationMappingTable.packageName] = packageName
                it[ObfuscationMappingTable.appVersionCode] = appVersionCode
                it[ObfuscationMappingTable.mapping] =
                    ExposedBlob(mappingFileContents.toByteArray(Charsets.UTF_8))
            }
        }
    }

    override suspend fun getMapping(packageName: String, appVersionCode: Long): String? {
        return suspendedTransaction(database) {

            ObfuscationMappingTable
                .slice(ObfuscationMappingTable.mapping)
                .select {
                    (ObfuscationMappingTable.packageName eq packageName) and
                            (ObfuscationMappingTable.appVersionCode eq appVersionCode)
                }
                .limit(1)
                .firstOrNull()
                ?.let { resultRow ->
                    resultRow[ObfuscationMappingTable.mapping]
                        .bytes
                        .toString(Charsets.UTF_8)
                }
        }
    }

    override suspend fun getStoredMappingsForPackageName(packageName: String): List<Long> {
        return suspendedTransaction(database) {

            ObfuscationMappingTable
                .slice(ObfuscationMappingTable.appVersionCode)
                .select { ObfuscationMappingTable.packageName eq packageName }
                .map { resultRow -> resultRow[ObfuscationMappingTable.appVersionCode] }
        }
    }

}