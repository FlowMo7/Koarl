package dev.moetz.koarl.backend.persistence.sql

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction


internal suspend inline fun <T> suspendedTransaction(
    database: Database,
    crossinline statement: Transaction.() -> T
): T {
    return newSuspendedTransaction(db = database) {
//        addLogger(StdOutSqlLogger)
        try {
            statement.invoke(this)
        } catch (throwable: Throwable) {
            System.err.println("Error in SQL statement:")
            throwable.printStackTrace()
            throw throwable
        }
    }
}