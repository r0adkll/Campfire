// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.db

import app.campfire.core.di.AppScope
import app.campfire.themes.CampfireThemeDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

expect interface SqlDelightDatabasePlatformComponent

@ContributesTo(AppScope::class)
interface ThemingDatabaseComponent : SqlDelightDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideSqlDelightDatabase(
    factory: ThemingDatabaseFactory,
  ): CampfireThemeDatabase {
    return factory.create()
  }
}
