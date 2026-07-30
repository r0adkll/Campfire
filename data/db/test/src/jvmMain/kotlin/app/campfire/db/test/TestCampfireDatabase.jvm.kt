// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.db.test

import app.campfire.CampfireDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual suspend fun createDriver(): app.cash.sqldelight.db.SqlDriver {
  return JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
    CampfireDatabase.Schema.synchronous().create(it)
  }
}
