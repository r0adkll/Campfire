// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.db

import android.app.Application
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import me.tatarka.inject.annotations.Provides

actual interface BookInfoDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideBookInfoDatabase(
    application: Application,
  ): BookInfoDatabase {
    application.databaseList()
      .filter { it.startsWith(BookInfoDatabaseFilePrefix) && !it.startsWith(BookInfoDatabaseFileName) }
      .forEach { application.deleteDatabase(it) }

    val driver = AndroidSqliteDriver(
      schema = BookInfoDatabase.Schema.synchronous(),
      context = application,
      name = BookInfoDatabaseFileName,
    )
    return BookInfoDatabase(driver)
  }
}
