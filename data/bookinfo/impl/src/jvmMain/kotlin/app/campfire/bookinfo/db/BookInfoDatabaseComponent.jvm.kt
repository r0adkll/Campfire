// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.db

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import me.tatarka.inject.annotations.Provides

actual interface BookInfoDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideBookInfoDatabase(): BookInfoDatabase {
    val userRoot = System.getProperty(
      "java.util.prefs.userRoot",
      System.getProperty("user.home"),
    )
    val appDir = File(File(userRoot), ".config/Campfire").apply { mkdirs() }

    appDir.listFiles()
      ?.filter { it.name.startsWith(BookInfoDatabaseFilePrefix) && !it.name.startsWith(BookInfoDatabaseFileName) }
      ?.forEach { it.delete() }

    val databaseFile = File(appDir, BookInfoDatabaseFileName)
    val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
    BookInfoDatabase.Schema
      .synchronous()
      .create(driver)
    return BookInfoDatabase(driver)
  }
}
