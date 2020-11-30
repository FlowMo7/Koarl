package dev.moetz.koarl.backend.grafana

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.joda.time.DateTime
import org.joda.time.Instant
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormatterBuilder


/**
 * Lambda, which gets invoked to retrieve the data for a metric with the given
 * [GrafanaDataSourceMapper.QuerySelection] for a **timeseries** graph.
 *
 * This lambda should return an [Iterable] of [GrafanaDataSourceMapper.DataPoint]
 * (or null if no data-points found).
 */
typealias TimeseriesSourceLambda = suspend (querySelection: GrafanaDataSourceMapper.QuerySelection) -> Iterable<GrafanaDataSourceMapper.DataPoint>?

/**
 * Lambda, which gets invoked to retrieve the data for a metric with the given
 * [GrafanaDataSourceMapper.QuerySelection] for a **table** graph.
 *
 * This lambda should return a [GrafanaDataSourceMapper.TableData] object on success
 * (or null if no data found).
 */
typealias TableSourceLambda = suspend (querySelection: GrafanaDataSourceMapper.QuerySelection) -> GrafanaDataSourceMapper.TableData?


/**
 * This class exposes an API to expose data to _Grafana_ using the json datasource plugin
 * (see https://grafana.com/grafana/plugins/simpod-json-datasource).
 *
 * Call [addTimeSeriesSource] or [addTableSource] to add data-source to be exposed to _Grafana_.
 *
 * Call [installRoute] with the ktor [Route] you want to inject the api into.
 *
 * @param json The [Json] for the API serialization.
 * **Note** that this should be a _nonStrict_ [Json] ([Json.nonstrict]), in contrast to what the
 * rest of the application should use.
 */
class GrafanaApi(
    private val json: Json
) {

    private val grafanaDataSourceMapper = GrafanaDataSourceMapper(json)

    private val timeSeriesSourceMap: MutableMap<String, TimeseriesSourceLambda> = mutableMapOf()
    private val tableSourceMap: MutableMap<String, TableSourceLambda> = mutableMapOf()


    /**
     * Registers a _timeseries_ grafana data source series which is discoverable in _Grafana_ with the given [name].
     *
     * The provided [dataSupplier] will be invoked to get the retrieve the data.
     * The [dataSupplier] will be invoked for every request done from _Grafana_.
     *
     * @param name The name of the series (which needs to be unique (also regarding [registerTableSource]).
     * @param dataSupplier A lambda which will be invoked for each time _Grafana_ requests data with
     * the given [GrafanaDataSourceMapper.QuerySelection].
     */
    fun registerTimeSeriesSource(name: String, dataSupplier: TimeseriesSourceLambda) {
        synchronized(timeSeriesSourceMap) {
            require(
                timeSeriesSourceMap.containsKey(name).not()
            ) { "timeseries source with name '$name' already added." }

            timeSeriesSourceMap[name] = dataSupplier
        }
    }

    /**
     * Registers a _table_ grafana data source series which is discoverable in _Grafana_ with the given [name].
     *
     * The provided [dataSupplier] will be invoked to get the retrieve the data.
     * The [dataSupplier] will be invoked for every request done from _Grafana_.
     *
     * @param name The name of the series (which needs to be unique (also regarding [registerTimeSeriesSource]).
     * @param dataSupplier A lambda which will be invoked for each time _Grafana_ requests data with
     * the given [GrafanaDataSourceMapper.QuerySelection].
     */
    fun registerTableSource(name: String, dataSupplier: TableSourceLambda) {
        synchronized(tableSourceMap) {
            require(
                tableSourceMap.containsKey(name).not()
            ) { "table source with name '$name' already added." }

            tableSourceMap[name] = dataSupplier
        }
    }


    /**
     * Installs the _Grafana_ API into the given [route].
     * There is no authentication done inside here, so the passed route should already be wrapped
     * in an authorization.
     *
     * @param route The route to add the API to.
     */
    fun installRoute(route: Route) {
        route.apply {

            // `/` should return 200 ok. Used for "Test connection" on the datasource config page.
            get {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = ""
                )
            }

            // `/search` used by the find metric options on the query tab in panels.
            post("search") {
                call.respondText(contentType = ContentType.Application.Json) {
                    (timeSeriesSourceMap.keys + tableSourceMap.keys)
                        .toList()
                        .joinToString(
                            separator = ",",
                            prefix = "[",
                            postfix = "]"
                        ) { "\"$it\"" }
                }
            }

            // `/query` should return metrics based on input.
            post("query") {
                val requestBodyString = call.receiveText()

                call.respondText(contentType = ContentType.Application.Json) {
                    grafanaDataSourceMapper.query(
                        requestBodyString,
                        timeseriesDataSupplier = { querySelection, target ->
                            timeSeriesSourceMap[target]?.invoke(querySelection)
                        },
                        tableDataSupplier = { querySelection, target ->
                            tableSourceMap[target]?.invoke(querySelection)
                        }
                    )
                }

            }

            // `/annotations` should return annotations.
            post("annotations") {
                call.respondText(contentType = ContentType.Application.Json) { "[]" }
            }

            get("annotations") {
                call.respondText(contentType = ContentType.Application.Json) { "[]" }
            }


            //OPTIONAL:
            // `/tag-keys` should return tag keys for ad hoc filters.
            // `/tag-values` should return tag values for ad hoc filters.

        }
    }

}


class GrafanaDataSourceMapper(
    private val json: Json
) {

    /**
     * The parameters for a query-request to fetch data for a graph.
     *
     * @param from The start of the timespan the data should be returned of.
     * @param to The end ot the timespan the data should be returned of.
     * @param interval The interval of the datapoints which should be returned.
     * @param maxDataPoints The maximum amount of datapoints to return
     */
    data class QuerySelection(
        val from: DateTime,
        val to: DateTime,
        val interval: Interval,
        val maxDataPoints: Long
    ) {

        /**
         * The interval of the datapoints which should be returned.
         *
         * @param humanReadable The interval as human-readable string (e.g. 20s)
         * @param milliseconds The interval in milli-seconds
         */
        data class Interval(
            val humanReadable: String?,
            val milliseconds: Long
        )
    }


    private class RequestBody {
        @Serializable
        internal data class Query(
//        val requestId: String,
//        val timezone: String?,
//        val panelId: Long?,
//        val dashboardId: Long?,
            val range: Range? = null,
            val interval: String? = null,
            val intervalMs: Long? = null,
            val targets: List<Target?>?,
            val maxDataPoints: Long? = null
//        val scopedVars:
//        val startTime: Long?,
//        val rangeRaw: Range.Raw?
        ) {
            @Serializable
            data class Range(
                val from: String? = null,
                val to: String? = null,
                val raw: Raw? = null
            ) {
                @Serializable
                data class Raw(
                    val from: String? = null,
                    val to: String? = null
                )
            }

            @Serializable
            data class Target(
                val target: String? = null,
                val refId: String? = null,
                val type: String? = null
            )
        }
    }


    data class TableData(
        val columns: List<Column>,
        val rows: Iterable<RowData>
    ) {
        data class Column(
            val title: String,
            val type: String
        )

        interface RowData {
            fun serializedItem(column: Column): String
        }
    }


    data class DataPoint(
        val metricValue: Float,
        val timestamp: Long
    ) {

        constructor(metricValue: Float, datetime: DateTime) : this(
            metricValue = metricValue,
            timestamp = datetime.millis
        )

        internal fun serialize(): String {
            return "[$metricValue,$timestamp]"
        }

        override fun toString(): String {
            return "DataPoint(timestamp: ${DateTime(Instant.ofEpochMilli(timestamp))}, metricValue: $metricValue)"
        }

    }


    private val dateTimeFormatter: DateTimeFormatter by lazy {
        DateTimeFormatterBuilder()
            .appendYear(4, 4)
            .appendLiteral('-')
            .appendMonthOfYear(2)
            .appendLiteral('-')
            .appendDayOfMonth(2)
            .appendLiteral('T')
            .appendHourOfDay(2)
            .appendLiteral(':')
            .appendMinuteOfHour(2)
            .appendLiteral(':')
            .appendSecondOfMinute(2)
            .appendLiteral('.')
            .appendMillisOfSecond(3)
            .appendLiteral('Z')
            .toFormatter()
    }

    private fun String.parseAsDateTime(): DateTime = DateTime.parse(this, dateTimeFormatter)


    suspend fun query(
        requestBody: String,
        timeseriesDataSupplier: suspend (querySelection: QuerySelection, target: String) -> Iterable<DataPoint>?,
        tableDataSupplier: suspend (querySelection: QuerySelection, target: String) -> TableData?
    ): String {
        val queryRequestBody = json.parse(RequestBody.Query.serializer(), requestBody)

        val from = queryRequestBody.range?.from?.parseAsDateTime()
        val to = queryRequestBody.range?.to?.parseAsDateTime()

        val interval = queryRequestBody.interval
        val intervalMs = queryRequestBody.intervalMs

        val maxDataPoints = queryRequestBody.maxDataPoints

        val targetResponseItems =
            if (from != null && to != null && intervalMs != null && maxDataPoints != null) {
                (queryRequestBody.targets ?: emptyList())
                    .filterNotNull()
                    .mapNotNull { requestBodyTarget ->
                        val target = requestBodyTarget.target
                        val type = requestBodyTarget.type

                        if (target != null && type != null) {
                            when (type) {
                                "timeserie" -> {
                                    val dataPoints = timeseriesDataSupplier.invoke(
                                        QuerySelection(
                                            from = from,
                                            to = to,
                                            interval = QuerySelection.Interval(
                                                humanReadable = interval,
                                                milliseconds = intervalMs
                                            ),
                                            maxDataPoints = maxDataPoints
                                        ),
                                        target
                                    )
                                    if (dataPoints != null) {
                                        "{" +
                                                "\"target\":\"${target}\"," +
                                                "\"datapoints\":" +
                                                "[${dataPoints.joinToString(separator = ",") { it.serialize() }}]" +
                                                "}"
                                    } else {
                                        null
                                    }
                                }
                                "table" -> {
                                    val tableData = tableDataSupplier.invoke(
                                        QuerySelection(
                                            from = from,
                                            to = to,
                                            interval = QuerySelection.Interval(
                                                humanReadable = interval,
                                                milliseconds = intervalMs
                                            ),
                                            maxDataPoints = maxDataPoints
                                        ),
                                        target
                                    )
                                    if (tableData != null) {
                                        "{" +
                                                "\"type\":\"table\"," +
                                                "\"columns\":" +
                                                "[${tableData.columns.joinToString(separator = ",") { "{\"text\":\"${it.title}\",\"type\":\"${it.type}\"}" }}]," +
                                                "\"rows\":" +
                                                tableData.rows.joinToString(
                                                    ",",
                                                    prefix = "[",
                                                    postfix = "]"
                                                ) { rowData ->
                                                    tableData.columns.joinToString(
                                                        ",",
                                                        prefix = "[",
                                                        postfix = "]"
                                                    ) { column -> rowData.serializedItem(column) }
                                                } +
                                                "}"
                                    } else {
                                        null
                                    }
                                }
                                else -> null
                            }
                        } else {
                            null
                        }
                    }
            } else {
                emptyList()
            }

        return StringBuilder()
            .apply {
                append(
                    targetResponseItems.joinToString(
                        separator = ",",
                        prefix = "[",
                        postfix = "]"
                    ) { serializedTargetItem ->
                        serializedTargetItem
                    })
            }
            .toString()
    }


}