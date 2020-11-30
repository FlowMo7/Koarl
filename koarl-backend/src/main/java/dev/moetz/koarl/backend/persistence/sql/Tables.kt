package dev.moetz.koarl.backend.persistence.sql

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.datetime

object CrashTable : Table("crashTable") {
    val uuid = uuid("uuid")
    val isFatal = bool("isFatal")
    val inForeground = bool("inForeground")
    val dateTime = datetime("dateTime")
    val throwable = text("throwable")

    val deviceData = (integer("deviceData") references DeviceDataTable.id).nullable()
    val appData = integer("appData") references AppDataTable.id
    val groupUuid = uuid("groupUuid")

    val freeMemory = long("freeMemory")
    val totalMemory = long("totalMemory")
    val orientation = enumeration("orientation", DeviceOrientation::class)

    enum class DeviceOrientation {
        Portrait, Landscape, Undefined
    }

    override val primaryKey = PrimaryKey(uuid)
}

object DeviceDataTable : Table("deviceDataTable") {
    val id = integer("id").autoIncrement()
    val deviceName = varchar("deviceName", 128)
    val manufacturer = varchar("manufacturer", 128)
    val brand = varchar("brand", 128)
    val model = varchar("model", 128)
    val buildId = varchar("buildId", 128)
    val operationSystemVersion = integer("operationSystemVersion")

    override val primaryKey = PrimaryKey(id)
}

object AppDataTable : Table("appDataTable") {
    val id = integer("id").autoIncrement()
    val packageName = varchar("packageName", 512)
    val appName = varchar("appName", 512)
    val appVersionCode = long("appVersionCode")
    val appVersionName = varchar("appVersionName", 512)

    override val primaryKey = PrimaryKey(id)
}


object ObfuscationMappingTable : Table("obfuscationMappingTable") {
    val packageName = varchar("packageName", 512)
    val appVersionCode = long("appVersionCode")
    val mapping = blob("mapping")

    override val primaryKey = PrimaryKey(packageName, appVersionCode)
}
