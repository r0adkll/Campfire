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
    factory.migrate()
    return factory.build()
  }
}
