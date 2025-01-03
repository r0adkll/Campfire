package app.campfire.db

import app.campfire.CampfireDatabase
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.bark
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import me.tatarka.inject.annotations.Provides

actual interface SqlDelightDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideJvmSqlDriver(): SqlDriver {
    val userRoot = System.getProperty(
      "java.util.prefs.userRoot",
      System.getProperty("user.home"),
    )
    val userDir = File(userRoot)
    val appDir = File(userDir, ".config/Campfire").apply { mkdirs() }
    val databaseFile = File(appDir, "campfire.db")

    bark { "Creating SqlDriver for Database: ${databaseFile.absolutePath}" }

    val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
//    DestructiveMigrationSchema.perform(driver)
    CampfireDatabase.Schema
      .synchronous()
      .create(driver)
    return driver
  }
}
