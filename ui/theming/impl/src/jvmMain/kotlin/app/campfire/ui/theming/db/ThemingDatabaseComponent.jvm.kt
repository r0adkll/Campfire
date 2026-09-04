// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.db

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.db.desktopSqliteDriver
import app.campfire.db.hasColumn
import app.campfire.db.setUserVersion
import app.campfire.themes.CampfireThemeDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import java.io.File
import me.tatarka.inject.annotations.Provides

actual interface SqlDelightDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @ThemingDb
  @Provides
  fun provideThemingJvmSqlDriver(): SqlDriver {
    val userRoot = System.getProperty(
      "java.util.prefs.userRoot",
      System.getProperty("user.home"),
    )
    val userDir = File(userRoot)
    val appDir = File(userDir, ".config/Campfire").apply { mkdirs() }
    val databaseFile = File(appDir, "campfire_themes.db")
    val schema = CampfireThemeDatabase.Schema.synchronous()

    return desktopSqliteDriver(databaseFile, schema) {
      // Older desktop builds never stamped a version. This database holds user-made themes, so
      // keep it: every table is `CREATE TABLE IF NOT EXISTS`, and the only migration so far
      // (1.sqm) added columns to customAppTheme, so apply it when those columns are missing.
      schema.create(this).value
      if (!hasColumn(table = "customAppTheme", column = "isAi")) {
        schema.migrate(this, 0, schema.version).value
      }
      setUserVersion(schema.version)
    }
  }
}
