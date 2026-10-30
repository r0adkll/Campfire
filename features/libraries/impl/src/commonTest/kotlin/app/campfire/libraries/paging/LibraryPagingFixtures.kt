// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingState
import app.campfire.CampfireDatabase
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress.Source
import app.campfire.core.model.MediaType
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.core.time.FatherTime
import app.campfire.data.Library
import app.campfire.data.MediaProgress
import app.campfire.network.PagedResponse
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.MediaMinified
import app.campfire.network.models.MediaType as NetworkMediaType
import app.campfire.network.models.MinifiedBookMetadata
import app.campfire.network.models.Podcast
import app.campfire.user.test.fixtures.user
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

internal val TestUser = user("user")

internal val TestInput = LibraryItemPagingInput(
  filter = null,
  sortMode = ContentSortMode.Title,
  sortDirection = SortDirection.Ascending,
)

/** When the mediator in these tests stores a page. */
internal const val LOADED_AT = 1_000_000L

internal class FixedFatherTime(var nowMillis: Long) : FatherTime {
  override fun now(): LocalDateTime = error("not used in tests")
  override fun today(): LocalDate = error("not used in tests")
  override fun nowInEpochMillis(): Long = nowMillis
}

internal suspend fun CampfireDatabase.library(mediaType: String) {
  librariesQueries.insert(
    Library(
      id = TestUser.selectedLibraryId,
      name = "Library",
      displayOrder = 0,
      icon = "database",
      mediaType = mediaType,
      provider = "",
      createdAt = 0,
      lastUpdate = 0,
      coverAspectRatio = 1,
      audiobooksOnly = false,
      userId = TestUser.id,
    ),
  )
}

/** Stores this user's progress on [episodeId] of the podcast [libraryItemId]. */
internal suspend fun CampfireDatabase.episodeProgress(
  libraryItemId: String,
  episodeId: String,
  isFinished: Boolean,
  finishedAt: Long?,
) {
  mediaProgressQueries.insert(
    MediaProgress(
      id = "progress-$episodeId",
      libraryItemId = libraryItemId,
      userId = TestUser.id,
      episodeId = episodeId,
      mediaItemId = episodeId,
      mediaItemType = MediaType.Podcast,
      duration = 100.0,
      progress = if (isFinished) 1.0 else 0.5,
      currentTime = 0.0,
      isFinished = isFinished,
      hideFromContinueListening = false,
      ebookLocation = null,
      ebookProgress = null,
      lastUpdate = finishedAt ?: 0,
      startedAt = 0,
      finishedAt = finishedAt,
      source = Source.Remote,
    ),
  )
}

internal fun emptyState() = PagingState<Int, LibraryItem>(
  pages = emptyList(),
  anchorPosition = null,
  config = PagingConfig(pageSize = PAGE_SIZE),
  leadingPlaceholderCount = 0,
)

internal fun page(vararg items: LibraryItemMinified) = PagedResponse(
  data = items.toList(),
  page = 0,
  limit = PAGE_SIZE,
  total = items.size,
  offset = 0,
)

internal fun podcast(
  id: String,
  numEpisodesIncomplete: Int = 0,
  numEpisodes: Int? = null,
) = LibraryItemMinified.Podcast(
  id = id,
  ino = "ino",
  libraryId = TestUser.selectedLibraryId,
  folderId = "folder",
  path = "path",
  relPath = "relPath",
  isFile = false,
  mtimeMs = 0,
  ctimeMs = 0,
  birthtimeMs = 0,
  addedAt = 0,
  updatedAt = 0,
  isMissing = false,
  isInvalid = false,
  mediaType = NetworkMediaType.Podcast2,
  size = 0,
  media = Podcast(id = "media-$id", numEpisodes = numEpisodes),
  numEpisodesIncomplete = numEpisodesIncomplete,
)

internal fun book(id: String) = LibraryItemMinified.Book(
  id = id,
  ino = "ino",
  libraryId = TestUser.selectedLibraryId,
  folderId = "folder",
  path = "path",
  relPath = "relPath",
  isFile = false,
  mtimeMs = 0,
  ctimeMs = 0,
  birthtimeMs = 0,
  addedAt = 0,
  updatedAt = 0,
  isMissing = false,
  isInvalid = false,
  mediaType = NetworkMediaType.Book,
  size = 0,
  media = MediaMinified(id = "media-$id", coverPath = null, metadata = MinifiedBookMetadata()),
)

private const val PAGE_SIZE = 50
