// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management

import app.campfire.account.api.UrlHydrator
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.management.api.cover.BookImage
import app.campfire.management.api.cover.CoverRepository
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.LibraryItemExpanded
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(UserScope::class)
@Inject
class ServerCoverRepository(
  private val api: AudioBookShelfApi,
  private val urlHydrator: UrlHydrator,
) : CoverRepository {

  override suspend fun searchCovers(provider: String, title: String, author: String?): Result<List<String>> {
    return api.searchCovers(
      provider = provider,
      title = title.trim(),
      author = author?.trim()?.takeIf { it.isNotEmpty() },
    ).map { covers -> covers.filter { it.isNotBlank() }.distinct() }
  }

  override suspend fun getBookImages(itemId: LibraryItemId): Result<List<BookImage>> {
    return api.getLibraryItem(itemId).map { item ->
      val coverPath = when (item) {
        is LibraryItemExpanded.Book -> item.media.coverPath
        is LibraryItemExpanded.Podcast -> item.media.coverPath
      }
      item.libraryFiles
        .filter { it.fileType == ImageFileType }
        .map { file ->
          BookImage(
            name = file.metadata.filename,
            path = file.metadata.path,
            url = urlHydrator.hydrateUrl("/api/items/$itemId/file/${file.ino}"),
            sizeBytes = file.metadata.size,
            isCover = file.metadata.path == coverPath,
          )
        }
    }
  }
}

private const val ImageFileType = "image"
