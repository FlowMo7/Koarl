package dev.moetz.koarl.android.dsl

import dev.moetz.koarl.android.ConfigurationDsl

data class PrivacySettings(
    val sendDeviceData: Boolean,
    val enableReporting: Boolean
) {
    class Builder @PublishedApi internal constructor() {

        private var sendDeviceData: Boolean = true
        private var enableReporting: Boolean = true

        /**
         * Defines whether on each fatal and non-fatal error report the current device state should
         * be captured and sent, too.
         */
        @Suppress("unused")
        @ConfigurationDsl
        fun sendDeviceData(sendDeviceData: Boolean) {
            this.sendDeviceData = sendDeviceData
        }

        /**
         * Defines whether Koarl should catch and send reports at all.
         * If you want to disable crash reporting at all, either due to debug/release builds, or due
         * to a user preference, set this flag to `false`.
         */
        @Suppress("unused")
        @ConfigurationDsl
        fun enableReporting(enableReporting: Boolean) {
            this.enableReporting = enableReporting
        }

        internal fun build(): PrivacySettings {
            return PrivacySettings(
                sendDeviceData = sendDeviceData,
                enableReporting = enableReporting
            )
        }
    }
}