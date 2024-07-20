package app.campfire.db

import app.campfire.CampfireDatabase
import app.campfire.core.di.AppScope
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import me.tatarka.inject.annotations.Provides

actual interface SqlDelightDatabasePlatformComponent {

  @AppScope
  @Provides
  fun provideNativeSqlDriver(): SqlDriver = NativeSqliteDriver(CampfireDatabase.Schema, "campfire.db")
}
