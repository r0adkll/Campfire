// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.db

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import me.tatarka.inject.annotations.Provides

actual interface BookInfoDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideBookInfoDatabase(): BookInfoDatabase {
    // Stale versioned files from prior schema versions are left in place on
    // native; they are tiny and the OS cleans caches under storage pressure.
    val driver = NativeSqliteDriver(
      schema = BookInfoDatabase.Schema.synchronous(),
      name = BookInfoDatabaseFileName,
    )
    return BookInfoDatabase(driver)
  }
}
