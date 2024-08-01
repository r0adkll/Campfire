package app.campfire.db

import app.campfire.CampfireDatabase
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import me.tatarka.inject.annotations.Provides

actual interface SqlDelightDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideJvmSqlDriver(): SqlDriver {
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    DestructiveMigrationSchema.perform(driver)
    CampfireDatabase.Schema.create(driver)
    return driver
  }
}
