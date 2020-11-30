package dev.moetz.koarl.backend.persistence.sql

import com.zaxxer.hikari.HikariConfig

sealed class DatabaseSource {

    protected abstract val hikariConfig: HikariConfig

    val config: HikariConfig
        get() {
            return hikariConfig.apply {
                validate()
            }
        }

    object H2InMemory : DatabaseSource() {
        override val hikariConfig: HikariConfig
            get() {
                return HikariConfig().apply {
                    driverClassName = "org.h2.Driver"
                    jdbcUrl = "jdbc:h2:mem:test"
                    maximumPoolSize = 3
                    isAutoCommit = false
                    transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                }
            }
    }

    class Jdbc(
        private val jdbcVerb: String,
        private val host: String,
        private val port: String,
        private val database: String,
        private val username: String,
        private val password: String
    ) : DatabaseSource() {
        enum class DriverClass(val type: String, val className: String) {
            MySQL("mysql", "com.mysql.cj.jdbc.Driver"),
            MariaDB("mariadb", "org.mariadb.jdbc.Driver"),
            PostgreSQL("postgresql", "org.postgresql.Driver")
        }

        override val hikariConfig: HikariConfig
            get() {
                return HikariConfig().apply {
                    driverClassName =
                        DriverClass.values().firstOrNull { it.type == it.type }?.className
                            ?: throw IllegalArgumentException(
                                "No driver found for $jdbcVerb. " +
                                        "Currently only ${DriverClass.values()
                                            .map { it.type }} are supported."
                            )
                    jdbcUrl = "jdbc:$jdbcVerb://$host:$port/$database"
                    username = this@Jdbc.username
                    password = this@Jdbc.password
                    maximumPoolSize = 3
                    isAutoCommit = false
                    transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                }
            }
    }

}
