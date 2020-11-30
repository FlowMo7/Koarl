package dev.moetz.koarl.backend.persistence.sql

import com.zaxxer.hikari.HikariDataSource
import dev.moetz.koarl.backend.persistence.AppStorage
import dev.moetz.koarl.backend.persistence.CrashStorage
import dev.moetz.koarl.backend.persistence.DatabaseProvider
import dev.moetz.koarl.backend.persistence.ObfuscationMappingStorage
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class SqlDatabaseProvider(
    private val json: Json,
    dataSource: HikariDataSource
) : DatabaseProvider {

//    private fun testConnection(dataSource: HikariDataSource) {
//        var round = 0
//        while (true) {
//            round++
//            println("Testing database connection" + (if (round != 1) " (round $round)" else ""))
//            try {
//                val testConfig = HikariConfig()
//                dataSource.copyStateTo(testConfig)
//                testConfig. maximumPoolSize = 1
//                Database.connect(HikariDataSource(testConfig))
//                println("Database connection test successful!")
//                return
//            } catch (exception: Exception) {
//                if (round < 5) {
//                    println("Error connecting to database (during connection test, database may still be starting up): " + exception.localizedMessage)
//                    Thread.sleep(5_000)
//                } else {
//                    System.err.println("Database connection failed. Tried connecting $round times without success")
//                    throw exception
//                }
//            }
//        }
//    }

    private val database: Database by lazy {

        Database.connect(dataSource).also {
            transaction(db = it) {
//                addLogger(StdOutSqlLogger)
                SchemaUtils.createMissingTablesAndColumns(
                    DeviceDataTable,
                    AppDataTable,
                    CrashTable,
                    ObfuscationMappingTable
                )
            }
        }
    }

    override val crashStorage: CrashStorage
        get() = SqlCrashStorage(
            json = json,
            database = database
        )


    override val appStorage: AppStorage
        get() = SqlAppStorage(
            database = database
        )


    override val obfuscationMappingStorage: ObfuscationMappingStorage
        get() = SqlObfuscationMappingStore(
            database = database
        )


}