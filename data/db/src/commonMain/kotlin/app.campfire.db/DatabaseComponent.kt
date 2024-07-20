package app.campfire.db

import app.campfire.CampfireDatabase
import app.campfire.core.di.AppScope
import app.campfire.core.di.MergeAppScope
import com.r0adkll.kotlininject.merge.annotations.ContributesTo
import me.tatarka.inject.annotations.Provides

expect interface SqlDelightDatabasePlatformComponent

@ContributesTo(MergeAppScope::class)
interface DatabaseComponent : SqlDelightDatabasePlatformComponent {
  @AppScope
  @Provides
  fun provideSqlDelightDatabase(
    factory: DatabaseFactory,
  ): CampfireDatabase = factory.build()
}
