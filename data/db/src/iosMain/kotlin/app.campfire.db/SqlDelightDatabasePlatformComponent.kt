package app.campfire.db

import app.campfire.CampfireDatabase
import app.campfire.core.di.AppScope
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

actual interface SqlDelightDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideNativeSqlDriver(): SqlDriver = NativeSqliteDriver(CampfireDatabase.Schema.synchronous(), "campfire.db")
}
