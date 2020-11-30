package dev.moetz.koarl.backend.koin

import com.zaxxer.hikari.HikariDataSource
import dev.moetz.koarl.backend.environment.EnvironmentVariable
import dev.moetz.koarl.backend.persistence.AppStorage
import dev.moetz.koarl.backend.persistence.CrashStorage
import dev.moetz.koarl.backend.persistence.DatabaseProvider
import dev.moetz.koarl.backend.persistence.ObfuscationMappingStorage
import dev.moetz.koarl.backend.persistence.sql.DatabaseSource
import dev.moetz.koarl.backend.persistence.sql.SqlDatabaseProvider
import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module
import java.net.InetAddress

val databaseModule = module {

    single<DatabaseProvider> {
        val environmentVariable: EnvironmentVariable = get()

        val inMemoryDatabase: Boolean =
            environmentVariable.contains(EnvironmentVariable.Key.Database.Sql.Type).not()

        val hikariConfig = if (inMemoryDatabase) {
            System.err.println("No '${EnvironmentVariable.Key.Database.Sql.Type.variable}' specified. Using the In-Memory database H2 (which is good for testing), but WILL NOT PERSIST ANY DATA ACCROSS RESTARTS!")
            DatabaseSource.H2InMemory.config
        } else {
            try {
                val host = environmentVariable.require(EnvironmentVariable.Key.Database.Sql.Host)

                val config = DatabaseSource.Jdbc(
                    jdbcVerb = environmentVariable.require(EnvironmentVariable.Key.Database.Sql.Type),
                    host = host,
                    port = environmentVariable.require(EnvironmentVariable.Key.Database.Sql.Port),
                    database = environmentVariable.require(EnvironmentVariable.Key.Database.Sql.DatabaseName),
                    username = environmentVariable.require(EnvironmentVariable.Key.Database.Sql.User),
                    password = environmentVariable.require(EnvironmentVariable.Key.Database.Sql.Password)
                ).config


                var count = 0
                var reachable = false
                var connectable = false
                while (reachable.not() && connectable.not() && count < 5) {
                    count++
                    reachable = try {
                        InetAddress.getByName(host).isReachable(5_000)
                    } catch(exception: Exception) {
                        System.err.println("database not reachable (count: $count): ${exception.localizedMessage}")
                        Thread.sleep(5_000)
                        false
                    }
                    println("$host reachable: $reachable")

                    if(reachable) {
                        connectable = try {
                            Database.connect(HikariDataSource(config))
                            true
                        }  catch(exception: Exception) {
                            System.err.println("database not connectable (count: $count): ${exception.localizedMessage}")
                            false
                        }
                        println("$host connectable: $connectable")
                    }
                }

                config
            } catch (exception: Exception) {
                throw IllegalArgumentException(
                    "Error getting all information for the database connection. " +
                            "Please check, that all necessary environment variables (${EnvironmentVariable.Key.Database.Sql.values()
                                .map { it.variable }}) are set. " +
                            exception.localizedMessage,
                    exception
                )
            }
        }

        SqlDatabaseProvider(
            json = get(),
            dataSource = HikariDataSource(hikariConfig)
        )
    }




    single<CrashStorage> { get<DatabaseProvider>().crashStorage }

    single<AppStorage> { get<DatabaseProvider>().appStorage }

    single<ObfuscationMappingStorage> { get<DatabaseProvider>().obfuscationMappingStorage }

}
