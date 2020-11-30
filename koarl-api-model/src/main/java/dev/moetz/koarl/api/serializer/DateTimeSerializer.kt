package dev.moetz.koarl.api.serializer

import kotlinx.serialization.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormatterBuilder

@Serializer(forClass = DateTime::class)
object DateTimeSerializer : KSerializer<DateTime> {

    private val oldFormatter: DateTimeFormatter =
        DateTimeFormatterBuilder()
            .appendYear(4, 4)
            .appendMonthOfYear(2)
            .appendDayOfMonth(2)
            .appendLiteral('T')
            .appendHourOfDay(2)
            .appendMinuteOfHour(2)
            .appendSecondOfMinute(2)
            .appendLiteral('.')
            .appendMillisOfSecond(3)
            .appendTimeZoneId()
            .toFormatter()

    private val formatter: DateTimeFormatter =
        DateTimeFormatterBuilder()
            .appendYear(4, 4)
            .appendLiteral('-')
            .appendMonthOfYear(2)
            .appendLiteral('-')
            .appendDayOfMonth(2)
            .appendLiteral('T')
            .appendHourOfDay(2)
            .appendMinuteOfHour(2)
            .appendSecondOfMinute(2)
            .appendLiteral('.')
            .appendMillisOfSecond(3)
            .appendTimeZoneId()
            .toFormatter()

    val formatString: String get() = "yyyy-MM-dd'T'HH:mm:ss.SSSV"


    fun serializeToString(dateTime: DateTime): String {
        return try {
            formatter.print(dateTime.withZone(DateTimeZone.UTC))
        } catch (e: IllegalArgumentException) {
            println("serializeToString: Normal formatter errored for $dateTime, trying oldFormatter")
            oldFormatter.print(dateTime.withZone(DateTimeZone.UTC))
        }
    }


    fun deserializeFromString(serialized: String): DateTime {
        return try {
            DateTime.parse(serialized, formatter).withZone(DateTimeZone.getDefault())
        } catch (e: IllegalArgumentException) {
            println("deserializeFromString: Normal formatter errored for $serialized, trying oldFormatter")
            DateTime.parse(serialized, oldFormatter).withZone(DateTimeZone.getDefault())
        }
    }


    override val descriptor: SerialDescriptor
        get() = PrimitiveDescriptor("ZonedDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DateTime) =
        encoder.encodeString(serializeToString(value))

    override fun deserialize(decoder: Decoder): DateTime {
        return deserializeFromString(decoder.decodeString())
    }

}