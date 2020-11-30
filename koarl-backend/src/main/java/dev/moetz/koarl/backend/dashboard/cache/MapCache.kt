package dev.moetz.koarl.backend.dashboard.cache

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import java.io.File

internal class MapCache<Value>(
    private val json: Json,
    private val serializer: KSerializer<Value>,
    private val cacheDirectory: File
) {

    private fun getCacheFile(key: String): File {
        return File(cacheDirectory.also { it.mkdirs() }, "$key.json")
    }

    operator fun set(key: String, list: List<Value>) {
        val cacheFile = getCacheFile(key)
        val string = json.stringify(serializer.list, list)
        synchronized(this) {
            cacheFile.writeText(string)
        }
    }

    operator fun get(key: String): List<Value> {
        val cacheFile = getCacheFile(key)
        return synchronized(this) {
            if (cacheFile.exists()) {
                val fileContent = cacheFile.readText()
                if (fileContent.isBlank()) {
                    emptyList()
                } else {
                    try {
                        json.parse(serializer.list, fileContent)
                    } catch (throwable: Throwable) {
                        throwable.printStackTrace()
                        emptyList<Value>()
                    }
                }
            } else {
                emptyList()
            }
        }
    }

}