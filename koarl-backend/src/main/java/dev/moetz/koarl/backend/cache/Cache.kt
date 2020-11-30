package dev.moetz.koarl.backend.cache

import org.joda.time.LocalDateTime

interface Cache<T> {

    interface UsagePredicate {
        fun shouldUseCacheEntry(
            currentDateTime: LocalDateTime,
            cacheItemDateTime: LocalDateTime
        ): Boolean

        companion object {
            fun isNotOlderThanDays(days: Int): UsagePredicate {
                return object : UsagePredicate {
                    override fun shouldUseCacheEntry(
                        currentDateTime: LocalDateTime,
                        cacheItemDateTime: LocalDateTime
                    ): Boolean = cacheItemDateTime.plusDays(days).isAfter(currentDateTime)
                }
            }

            fun isNotOlderThanHours(hours: Int): UsagePredicate {
                return object : UsagePredicate {
                    override fun shouldUseCacheEntry(
                        currentDateTime: LocalDateTime,
                        cacheItemDateTime: LocalDateTime
                    ): Boolean = cacheItemDateTime.plusHours(hours).isAfter(currentDateTime)
                }
            }

            fun isNotOlderThanMinutes(minutes: Int): UsagePredicate {
                return object : UsagePredicate {
                    override fun shouldUseCacheEntry(
                        currentDateTime: LocalDateTime,
                        cacheItemDateTime: LocalDateTime
                    ): Boolean = cacheItemDateTime.plusMinutes(minutes).isAfter(currentDateTime)
                }
            }

            fun isOnSameDay(): UsagePredicate {
                return object : UsagePredicate {
                    override fun shouldUseCacheEntry(
                        currentDateTime: LocalDateTime,
                        cacheItemDateTime: LocalDateTime
                    ): Boolean =
                        currentDateTime.toLocalDate().isEqual(cacheItemDateTime.toLocalDate())
                }
            }
        }
    }

    fun get(cacheKey: String, elseReceiver: () -> T): T

    fun getNullable(cacheKey: String, elseReceiver: () -> T?): T?

    fun set(cacheKey: String, item: T)

}