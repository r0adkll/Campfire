// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.db

import app.campfire.core.di.AppScope
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

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
