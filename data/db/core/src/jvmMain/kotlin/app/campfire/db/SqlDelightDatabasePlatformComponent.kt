// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.db

import app.campfire.CampfireDatabase
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import java.io.File
import me.tatarka.inject.annotations.Provides

actual interface SqlDelightDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideJvmSqlDriver(): SqlDriver {
    val userRoot = System.getProperty(
      "java.util.prefs.userRoot",
      System.getProperty("user.home"),
    )
    val userDir = File(userRoot)
    val appDir = File(userDir, ".config/Campfire").apply { mkdirs() }
    val databaseFile = File(appDir, "campfire.db")
    val schema = CampfireDatabase.Schema.synchronous()

    return desktopSqliteDriver(databaseFile, schema) {
      // Older desktop builds re-ran `create` on every launch and never stamped a version, so
      // the shape of such a file is unknown. It is only a cache of server data, so rebuild it.
      rebuild(schema)
    }
  }
}
