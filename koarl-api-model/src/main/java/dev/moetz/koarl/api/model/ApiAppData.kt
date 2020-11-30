package dev.moetz.koarl.api.model

import kotlinx.serialization.Serializable


@Serializable
data class ApiAppData(
    val packageName: String,
    val appName: String,
    val appVersionCode: Long,
    val appVersionName: String
)