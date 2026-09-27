// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.db

import app.campfire.core.di.AppScope
import app.campfire.themes.CampfireThemeDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

actual interface SqlDelightDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @ThemingDb
  @Provides
  fun provideThemingNativeSqlDriver(): SqlDriver {
    return NativeSqliteDriver(CampfireThemeDatabase.Schema.synchronous(), "campfire_themes.db")
  }
}
