package dev.moetz.koarl.backend.grafana

import dev.moetz.koarl.backend.cache.Cache
import dev.moetz.koarl.backend.cache.MemoryCache
import dev.moetz.koarl.backend.persistence.AppStorage
import dev.moetz.koarl.backend.persistence.CrashStorage
import kotlinx.coroutines.runBlocking
import org.joda.time.Duration
import org.joda.time.Instant
import org.joda.time.Interval
import kotlin.math.max


/**
 * Initializes data-sources when calling [init] on the provided [grafanaApi], which triggers
 * databases-queries in here.
 *
 * @param crashStorage The [CrashStorage] to get the persisted crashes
 * @param appStorage The [AppStorage] to get the persisted app versions
 * @param grafanaApi The [GrafanaApi] to register the data-sources on
 */
class GrafanaDataSource(
    private val crashStorage: CrashStorage,
    private val appStorage: AppStorage,
    private val grafanaApi: GrafanaApi
) {

    private object Metrics {
        const val NUMBER_OF_CRASHES = "numberOfCrashes"
    }


    internal suspend fun init() {
        val metrics = listOf(
            Metrics.NUMBER_OF_CRASHES
        )


        val packageNameList = appStorage.getStoredApps().distinct().map { (packageName, _) ->
            packageName to appStorage.getStoredVersionsForPackageName(packageName).map { it.appVersionName }.distinct()
        }

        metrics
            .flatMap { metric ->
                listOf(metric) + packageNameList.flatMap { (packageName, appVersionList) ->
                    listOf("$metric:$packageName") + appVersionList.map { "$metric:$packageName:${it}" }
                }
            }
            .forEach { metricName ->
                grafanaApi.registerTimeSeriesSource(name = metricName) { querySelection ->
                    timeseriesDataSupplier(querySelection, metricName)
                }
            }
    }


    private suspend fun timeseriesDataSupplier(
        querySelection: GrafanaDataSourceMapper.QuerySelection,
        target: String
    ): Iterable<GrafanaDataSourceMapper.DataPoint>? {
        val requestTarget = KoarlGrafanaRequestTarget.getFromTarget(target)

        return when (requestTarget.metric) {
            Metrics.NUMBER_OF_CRASHES -> {
                getNumberOfCrashes(
                    querySelection,
                    requestTarget.packageName,
                    requestTarget.appVersionName
                )
            }
            else -> {
                println("ERROR: no metric found in query for target '$target'")
                null
            }
        }
    }

    private val numberOfCrashesCache: Cache<List<GrafanaDataSourceMapper.DataPoint>> =
        MemoryCache(cacheUsagePredicate = Cache.UsagePredicate.isNotOlderThanMinutes(10))

    private suspend fun getNumberOfCrashes(
        querySelection: GrafanaDataSourceMapper.QuerySelection,
        packageName: String?,
        appVersionName: String?
    ): List<GrafanaDataSourceMapper.DataPoint> {
        return numberOfCrashesCache.get("${packageName}_${appVersionName}_${querySelection.from.toDate()}_${querySelection.to.toDate()}") {
            runBlocking {
                val crashes = crashStorage
                    .getCrashes(
                        packageName = packageName,
                        from = querySelection.from,
                        to = querySelection.to
                    )
                    .let { crashes ->
                        if (appVersionName != null) {
                            crashes.filter { it.appData.packageName == packageName }
                        } else {
                            crashes
                        }
                    }

                dataListToIntervalCountList(
                    querySelection,
                    crashes,
                    minimumPeriod = Duration.standardMinutes(5)
                ) { crashData, interval -> interval.contains(crashData.crash.dateTime) }
            }
        }
    }


    private inline fun <T> dataListToIntervalCountList(
        querySelection: GrafanaDataSourceMapper.QuerySelection,
        dataList: List<T>,
        minimumPeriod: Duration? = null,
        isDataItemInIntervalPredicate: (item: T, interval: Interval) -> Boolean
    ): List<GrafanaDataSourceMapper.DataPoint> {
        val durationToIncrease = if (minimumPeriod != null) {
            Duration.millis(
                max(
                    querySelection.interval.milliseconds,
                    minimumPeriod.millis
                )
            )
        } else {
            Duration.millis(querySelection.interval.milliseconds)
        }

        val datapoints = (0 until querySelection.maxDataPoints)
            .mapNotNull { index ->
                val datetime =
                    querySelection.from.plusMillis((index * durationToIncrease.millis).toInt())

                if (datetime.isBefore(querySelection.to)) {
                    val interval = Interval(
                        datetime.minusMillis(durationToIncrease.millis.toInt()),
                        datetime
                    )

                    val numberOfItemsInInterval =
                        dataList.count { item ->
                            isDataItemInIntervalPredicate.invoke(
                                item,
                                interval
                            )
                        }

                    GrafanaDataSourceMapper.DataPoint(
                        metricValue = numberOfItemsInInterval.toFloat(),
                        datetime = datetime
                    )
                } else {
                    null
                }
            }

        return if (datapoints.last().timestamp != querySelection.to.millis) {
            datapoints + GrafanaDataSourceMapper.DataPoint(
                metricValue = dataList.count { item ->
                    isDataItemInIntervalPredicate.invoke(
                        item,
                        Interval(
                            Instant(datapoints.last().timestamp),
                            querySelection.to
                        )
                    )
                }.toFloat(),
                datetime = querySelection.to
            )
        } else {
            datapoints
        }
    }


    private class KoarlGrafanaRequestTarget(
        val metric: String,
        val packageName: String?,
        val appVersionName: String?
    ) {
        companion object {
            fun getFromTarget(target: String): KoarlGrafanaRequestTarget {
                val split = target.split(":", limit = 3)
                return KoarlGrafanaRequestTarget(
                    metric = split.getOrNull(0)
                        ?: throw IllegalArgumentException("At least metric needs to be set"),
                    packageName = split.getOrNull(1),
                    appVersionName = split.getOrNull(2)
                )
            }
        }
    }

}