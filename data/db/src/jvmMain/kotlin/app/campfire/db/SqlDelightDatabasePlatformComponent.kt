package app.campfire.db

import app.campfire.CampfireDatabase
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.bark
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import me.tatarka.inject.annotations.Provides

actual interface SqlDelightDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideJvmSqlDriver(): SqlDriver {
    val userDir = File(System.getProperty("user.dir"))
    val appDir = File(userDir, ".campfire").apply { mkdirs() }
    val databaseFile = File(appDir, "campfire.db")

    bark { "Creating SqlDriver for Database: ${databaseFile.absolutePath}" }

    val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
    DestructiveMigrationSchema.perform(driver)
    CampfireDatabase.Schema.create(driver)
    return driver
  }
}
