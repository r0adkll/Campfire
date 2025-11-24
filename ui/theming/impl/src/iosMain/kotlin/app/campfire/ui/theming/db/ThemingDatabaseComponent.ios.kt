package app.campfire.ui.theming.db

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.themes.CampfireThemeDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import me.tatarka.inject.annotations.Provides

actual interface SqlDelightDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @ThemingDb
  @Provides
  fun provideThemingNativeSqlDriver(): SqlDriver {
    return NativeSqliteDriver(CampfireThemeDatabase.Schema.synchronous(), "campfire_themes.db")
  }
}
