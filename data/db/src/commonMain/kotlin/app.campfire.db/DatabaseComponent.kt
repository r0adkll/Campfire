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
  ): CampfireDatabase = factory.build()
}
