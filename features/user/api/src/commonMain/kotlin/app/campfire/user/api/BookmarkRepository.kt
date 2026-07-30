// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.api

import app.campfire.core.model.Bookmark
import app.campfire.core.model.LibraryItemId
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {

  fun observeBookmarks(libraryItemId: LibraryItemId): Flow<List<Bookmark>>

  suspend fun createBookmark(
    libraryItemId: LibraryItemId,
    timestamp: Duration,
    title: String,
  )

  suspend fun removeBookmark(
    libraryItemId: LibraryItemId,
    timestamp: Duration,
  )
}
