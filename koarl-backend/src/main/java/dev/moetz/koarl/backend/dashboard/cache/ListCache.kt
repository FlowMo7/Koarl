package dev.moetz.koarl.backend.dashboard.cache

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import java.io.File

internal class ListCache<T>(
    private val json: Json,
    private val serializer: KSerializer<T>,
    private val cacheFile: File
) {

    val isLoaded: Boolean get() = cacheFile.exists()

    fun put(list: List<T>) {
        cacheFile.also { it.parentFile.mkdirs() }
        val string = json.stringify(serializer.list, list)
        synchronized(this) {
            cacheFile.writeText(string)
        }
    }

    fun get(): List<T> {
        return synchronized(this) {
            if (isLoaded) {
                val fileContent = cacheFile.readText()
                if (fileContent.isBlank()) {
                    emptyList<T>()
                } else {
                    try {
                        json.parse(serializer.list, fileContent)
                    } catch (throwable: Throwable) {
                        throwable.printStackTrace()
                        emptyList<T>()
                    }
                }
            } else {
                emptyList<T>()
            }
        }
    }

}