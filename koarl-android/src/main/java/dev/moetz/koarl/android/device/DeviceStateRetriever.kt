package dev.moetz.koarl.android.device

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Debug
import dev.moetz.koarl.api.model.ApiDeviceData
import dev.moetz.koarl.api.model.ApiDeviceState

/**
 * Interface for a component which can retrieve the current device state
 * (in the case of a fatal / non-fatal crash), to get more insights for the environment of the crash.
 *
 * The default implementation is implemented in the [AndroidDeviceStateRetriever].
 */
interface DeviceStateRetriever {

    /**
     * Called to retrieve the current [ApiDeviceState].
     *
     * The values returned should reflect the current status of the device when being called.
     *
     * @return An instance of [ApiDeviceState] which contains the current values of the given fields.
     */
    fun getDeviceState(): ApiDeviceState

    /**
     * Called to retrieve the device data specific fields. Those are most likely static during an
     * application lifecycle, so don't need to be calculated / summoned dynamically when calling
     * this method, but can be cached upfront.
     */
    fun getDeviceData(): ApiDeviceData

}

class AndroidDeviceStateRetriever(
    context: Context
) : DeviceStateRetriever {

    private val resources: Resources = context.resources

    data class MemoryDetails(
        val free: Long,
        val total: Long
    )


    override fun getDeviceState(): ApiDeviceState {
        val memoryDetails = getMemoryDetails()
        return ApiDeviceState(
            freeMemory = memoryDetails.free,
            totalMemory = memoryDetails.total,
            orientation = getOrientation()
        )
    }

    private fun getOrientation(): ApiDeviceState.ApiDeviceOrientation {
        return when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> ApiDeviceState.ApiDeviceOrientation.Landscape
            Configuration.ORIENTATION_PORTRAIT -> ApiDeviceState.ApiDeviceOrientation.Portrait
            Configuration.ORIENTATION_UNDEFINED -> ApiDeviceState.ApiDeviceOrientation.Undefined
            else -> ApiDeviceState.ApiDeviceOrientation.Undefined
        }
    }


    private fun getMemoryDetails(): MemoryDetails {
        return if (Build.VERSION.SDK_INT >= 26) {
            getAndroidOMemoryDetails()
        } else {
            getPreAndroidOMemoryDetails()
        }
    }

    @TargetApi(26)
    private fun getAndroidOMemoryDetails(): MemoryDetails {
        return Runtime.getRuntime().let { runtime ->
            val maxHeapSize = runtime.maxMemory()
            val availHeapSize = maxHeapSize - (runtime.totalMemory() - runtime.freeMemory())

            MemoryDetails(
                free = availHeapSize,
                total = maxHeapSize
            )
        }
    }

    private fun getPreAndroidOMemoryDetails(): MemoryDetails {
        return MemoryDetails(
            free = Debug.getNativeHeapSize(),
            total = Debug.getNativeHeapFreeSize()
        )
    }


    private val deviceData = ApiDeviceData(
        deviceName = Build.DEVICE,
        manufacturer = Build.MANUFACTURER,
        brand = Build.BRAND,
        model = Build.MODEL,
        buildId = Build.DISPLAY,
        operationSystemVersion = Build.VERSION.SDK_INT
    )

    override fun getDeviceData(): ApiDeviceData = deviceData


}