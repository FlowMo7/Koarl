package dev.moetz.koarl.api.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiDeviceState(
    val freeMemory: Long,
    val totalMemory: Long,
    val orientation: ApiDeviceOrientation
) {
    enum class ApiDeviceOrientation {
        Portrait, Landscape, Undefined
    }
}