package dev.moetz.koarl.backend.cache

import org.joda.time.Instant
import org.joda.time.LocalDateTime

class MemoryCache<T>(
    @PublishedApi
    internal val cacheUsagePredicate: Cache.UsagePredicate
) : Cache<T> {

    @PublishedApi
    internal val cache: MutableMap<String, Pair<LocalDateTime, T>> = mutableMapOf()

    override fun get(cacheKey: String, elseReceiver: () -> T): T {
        val cacheLastUpdate = lastCacheUpdate(cacheKey) ?: LocalDateTime(Instant.EPOCH)
        return if (cacheUsagePredicate.shouldUseCacheEntry(LocalDateTime.now(), cacheLastUpdate)) {
            cache[cacheKey]?.second ?: elseReceiver.invoke().also { set(cacheKey, it) }
        } else {
            elseReceiver.invoke().also { set(cacheKey, it) }
        }.also {
            cleanUpOutdatedItems()
        }
    }

    override fun getNullable(cacheKey: String, elseReceiver: () -> T?): T? {
        val cacheLastUpdate = lastCacheUpdate(cacheKey) ?: LocalDateTime(Instant.EPOCH)
        return if (cacheUsagePredicate.shouldUseCacheEntry(LocalDateTime.now(), cacheLastUpdate)) {
            cache[cacheKey]?.second ?: elseReceiver.invoke()?.also { set(cacheKey, it) }
        } else {
            elseReceiver.invoke()?.also { set(cacheKey, it) }
        }.also {
            cleanUpOutdatedItems()
        }
    }

    override fun set(cacheKey: String, item: T) {
        cache[cacheKey] = LocalDateTime.now() to item
    }

    @PublishedApi
    internal fun lastCacheUpdate(cacheKey: String): LocalDateTime? {
        return cache[cacheKey]?.first
    }

    @PublishedApi
    internal fun cleanUpOutdatedItems() {
        cache.entries.asSequence()
            .filter {
                cacheUsagePredicate.shouldUseCacheEntry(LocalDateTime.now(), it.value.first).not()
            }
            .map { it.key }
            .toList()
            .forEach { key -> cache.remove(key) }
    }
}