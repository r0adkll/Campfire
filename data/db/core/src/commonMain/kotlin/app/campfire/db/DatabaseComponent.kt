// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.db

import app.campfire.CampfireDatabase
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.r0adkll.kimchi.annotations.ContributesTo
import me.tatarka.inject.annotations.Provides

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
