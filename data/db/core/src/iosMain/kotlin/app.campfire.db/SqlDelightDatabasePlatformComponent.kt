package app.campfire.db

import app.campfire.CampfireDatabase
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import me.tatarka.inject.annotations.Provides

actual interface SqlDelightDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideNativeSqlDriver(): SqlDriver = NativeSqliteDriver(CampfireDatabase.Schema.synchronous(), "campfire.db")
}
