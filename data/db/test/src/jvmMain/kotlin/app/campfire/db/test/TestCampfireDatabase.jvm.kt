package app.campfire.db.test

import app.campfire.CampfireDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual suspend fun createDriver(): app.cash.sqldelight.db.SqlDriver {
  return JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
    CampfireDatabase.Schema.synchronous().create(it)
  }
}
