// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.db

import android.app.Application
import androidx.sqlite.db.SupportSQLiteDatabase
import app.campfire.CampfireDatabase
import app.campfire.core.di.AppScope
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

actual interface SqlDelightDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideAndroidSqlDriver(
    application: Application,
  ): SqlDriver = AndroidSqliteDriver(
    schema = CampfireDatabase.Schema.synchronous(),
    context = application,
    name = "campfire.db",
    callback = object : AndroidSqliteDriver.Callback(CampfireDatabase.Schema.synchronous()) {
      override fun onConfigure(db: SupportSQLiteDatabase) {
        db.enableWriteAheadLogging()
      }

      // Enforce foreign keys only once migrations have run, as the desktop and iOS drivers never
      // enforce them. A migration that rebuilds a table drops the old one, and with enforcement
      // on that drop deletes every row first, cascading through each table that references it.
      override fun onOpen(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
      }
    },
  )
}
