package dev.moetz.koarl.api.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiDeviceData(
    val deviceName: String,
    val manufacturer: String,
    val brand: String,
    val model: String,
    val buildId: String,
    val operationSystemVersion: Int
)