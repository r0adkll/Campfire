package app.campfire.ui.theming.db

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.themes.CampfireThemeDatabase
import com.r0adkll.kimchi.annotations.ContributesTo
import me.tatarka.inject.annotations.Provides

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
