// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.db

import app.campfire.CampfireDatabase
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

expect interface SqlDelightDatabasePlatformComponent

@ContributesTo(AppScope::class)
interface DatabaseComponent : SqlDelightDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideSqlDelightDatabase(
    factory: DatabaseFactory,
  ): CampfireDatabase {
    return factory.build()
  }
}
