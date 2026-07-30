// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.db.test

import app.campfire.CampfireDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual suspend fun createDriver(): app.cash.sqldelight.db.SqlDriver {
  return NativeSqliteDriver(
    CampfireDatabase.Schema.synchronous(),
    "test_db",
    onConfiguration = {
      it.copy(
        inMemory = true,
      )
    },
  )
}
