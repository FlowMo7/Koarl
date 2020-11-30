package dev.moetz.koarl.backend.persistence

interface ObfuscationMappingStorage {

    suspend fun insertMapping(
        packageName: String,
        appVersionCode: Long,
        mappingFileContents: String
    )

    suspend fun getMapping(packageName: String, appVersionCode: Long): String?

    suspend fun getStoredMappingsForPackageName(packageName: String): List<Long>

}