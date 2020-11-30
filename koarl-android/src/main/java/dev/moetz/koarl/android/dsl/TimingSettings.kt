package dev.moetz.koarl.android.dsl

import dev.moetz.koarl.android.ConfigurationDsl
import java.util.concurrent.TimeUnit

data class TimingSettings(
    internal val delayAfterApplicationStartToUpload: Long,
    internal val delayToRetryUpload: Long
) {
    class Builder @PublishedApi internal constructor() {

        internal var delayAfterApplicationStartToUpload: Long? = null
        internal var delayToRetryUpload: Long? = null

        /**
         * The delay after application start, after which the first crash-upload task should be
         * performed (if there are locally cached crashes present).
         *
         * Default value is 5 seconds.
         *
         * @param delay The delay to wait before uploading crashes after application start
         * @param unit The unit of the [delay] parameter.
         */
        @Suppress("unused")
        @ConfigurationDsl
        fun delayAfterApplicationStartToUpload(delay: Long, unit: TimeUnit) {
            this.delayAfterApplicationStartToUpload = unit.toMillis(delay)
        }

        /**
         * The delay after which failed / concurrent calls to upload crashes should be retried.
         *
         * Default value is 1 minute.
         *
         * @param delay The delay to wait after a failed upload to try again.
         * @param unit The unit of the [delay] parameter.
         * @
         */
        @Suppress("unused")
        @ConfigurationDsl
        fun delayToRetryUpload(delay: Long, unit: TimeUnit) {
            this.delayToRetryUpload = unit.toMillis(delay)
        }

        internal fun build(): TimingSettings {
            return TimingSettings(
                delayAfterApplicationStartToUpload = delayAfterApplicationStartToUpload
                    ?: 5_000, // default: 5 seconds
                delayToRetryUpload = delayToRetryUpload ?: (5 * 60_000) //default: 5 minutes
            )
        }
    }
}