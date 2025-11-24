package app.campfire.ui.theming.db

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.themes.CampfireThemeDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import me.tatarka.inject.annotations.Provides

actual interface SqlDelightDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @ThemingDb
  @Provides
  fun provideThemingJvmSqlDriver(): SqlDriver {
    val userRoot = System.getProperty(
      "java.util.prefs.userRoot",
      System.getProperty("user.home"),
    )
    val userDir = File(userRoot)
    val appDir = File(userDir, ".config/Campfire").apply { mkdirs() }
    val databaseFile = File(appDir, "campfire_themes.db")

//    bark { "Creating SqlDriver for Database: ${databaseFile.absolutePath}" }

    val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
//    DestructiveMigrationSchema.perform(driver)
    CampfireThemeDatabase.Schema
      .synchronous()
      .create(driver)
    return driver
  }
}
