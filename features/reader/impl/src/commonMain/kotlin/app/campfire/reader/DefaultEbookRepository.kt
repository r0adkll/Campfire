// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.reader.api.Ebook
import app.campfire.reader.api.EbookRepository
import app.campfire.user.api.MediaProgressRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Opens ebooks from this device: a downloaded book where there is one (see [EbookDownloader]),
 * otherwise the cached copy, fetched into the cache the first time the book is opened.
 */
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultEbookRepository(
  private val libraryItemRepository: LibraryItemRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val files: EbookFiles,
) : EbookRepository {

  override suspend fun open(itemId: LibraryItemId): Result<Ebook> = runCatching {
    val item = libraryItemRepository.getLibraryItem(itemId)
    val format = requireNotNull(item.media.ebookFormat) { "Item $itemId has no ebook" }
    val downloaded = files.downloaded(itemId, format)
    val cached = files.cached(itemId, format)
    val file = when {
      downloaded.hasContent() -> downloaded
      cached.hasContent() -> cached
      else -> cached.also { files.fetch(itemId, it) }
    }

    // A fresh fetch keeps whichever of this device's and the server's positions is newer, so one
    // saved by another client is picked up; offline, the last one saved here is used
    val progress = mediaProgressRepository.getProgress(itemId, fresh = true)
      ?: mediaProgressRepository.getProgress(itemId)
    Ebook(
      itemId = itemId,
      title = item.media.metadata.title.orEmpty(),
      author = item.media.metadata.authorName,
      format = format,
      filePath = file.toString(),
      location = progress?.ebookLocation,
      progress = progress?.ebookProgress,
    )
  }

  override suspend fun saveProgress(itemId: LibraryItemId, location: String, progress: Float): Result<Unit> =
    runCatching { mediaProgressRepository.updateEbookProgress(itemId, location, progress) }
}
