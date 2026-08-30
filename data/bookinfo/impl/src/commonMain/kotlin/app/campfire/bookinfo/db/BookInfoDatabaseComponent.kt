// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.db

import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesTo

/**
 * Platform components provide the [BookInfoDatabase] directly. This database is
 * a disposable third-party cache with a destructive migration strategy: the
 * database file name is versioned by the schema version, so any schema change
 * lands in a fresh file and stale files are cleaned up opportunistically —
 * there are no .sqm migrations to maintain.
 */
expect interface BookInfoDatabasePlatformComponent

@ContributesTo(AppScope::class)
interface BookInfoDatabaseComponent : BookInfoDatabasePlatformComponent

internal val BookInfoDatabaseFileName: String
  get() = "bookinfo_v${BookInfoDatabase.Schema.version}.db"

internal const val BookInfoDatabaseFilePrefix = "bookinfo_v"
