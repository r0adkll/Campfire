// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.test

import app.campfire.core.model.Bookmark
import app.campfire.core.model.LibraryItemId
import app.campfire.user.api.BookmarkRepository
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBookmarkRepository : BookmarkRepository {

  val invocations = mutableListOf<Invocation>()

  val bookmarksFlow = MutableStateFlow<List<Bookmark>>(emptyList())
  override fun observeBookmarks(libraryItemId: LibraryItemId): Flow<List<Bookmark>> {
    invocations += Invocation.ObserveBookmarks(libraryItemId)
    return bookmarksFlow
  }

  override suspend fun createBookmark(
    libraryItemId: LibraryItemId,
    timestamp: Duration,
    title: String,
  ) {
    invocations += Invocation.CreateBookmark(libraryItemId, timestamp, title)
  }

  override suspend fun removeBookmark(
    libraryItemId: LibraryItemId,
    timestamp: Duration,
  ) {
    invocations += Invocation.RemoveBookmark(libraryItemId, timestamp)
  }

  sealed interface Invocation {
    data class ObserveBookmarks(val libraryItemId: LibraryItemId) : Invocation
    data class CreateBookmark(
      val libraryItemId: LibraryItemId,
      val timestamp: Duration,
      val title: String,
    ) : Invocation
    data class RemoveBookmark(
      val libraryItemId: LibraryItemId,
      val timestamp: Duration,
    ) : Invocation
  }
}
