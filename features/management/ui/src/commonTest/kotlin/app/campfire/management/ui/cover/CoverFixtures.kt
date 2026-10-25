// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.cover

import androidx.compose.foundation.text.input.TextFieldState
import app.campfire.core.model.LibraryItemId
import app.campfire.management.api.cover.BookImage
import app.campfire.management.api.cover.CoverRepository
import app.campfire.management.api.match.ImageSize
import app.campfire.management.ui.editor.StagedCover
import app.campfire.management.ui.match.FakeMetadataMatchRepository
import kotlinx.coroutines.CompletableDeferred

/** The server's covers: what each source finds, by its id, and the book's own images. */
class FakeCoverRepository : CoverRepository {

  data class Search(val provider: String, val title: String, val author: String?)

  val searches = mutableListOf<Search>()

  /** What each source finds, by its id; a source not here finds nothing */
  val covers = mutableMapOf<String, Result<List<String>>>()

  /** Sources that wait for these before answering, as slow ones would */
  val gates = mutableMapOf<String, CompletableDeferred<Unit>>()

  var bookImagesResult: Result<List<BookImage>> = Result.success(emptyList())
  var bookImageLoads = 0

  override suspend fun searchCovers(provider: String, title: String, author: String?): Result<List<String>> {
    searches += Search(provider, title, author)
    gates[provider]?.await()
    return covers[provider] ?: Result.success(emptyList())
  }

  override suspend fun getBookImages(itemId: LibraryItemId): Result<List<BookImage>> {
    bookImageLoads++
    return bookImagesResult
  }
}

fun bookImage(
  name: String = "folder.jpg",
  isCover: Boolean = false,
) = BookImage(
  name = name,
  path = "/audiobooks/Children of Ruin/$name",
  url = "https://abs.example/api/items/book/file/$name",
  sizeBytes = 1_400_000,
  isCover = isCover,
)

fun coverUiState(
  currentUrl: String? = "https://abs.example/api/items/book/cover",
  currentSize: ImageSize? = null,
  staged: StagedCover? = null,
  mode: CoverMode = CoverMode.Search,
  results: List<CoverResults> = emptyList(),
  sizes: Map<String, ImageSize> = emptyMap(),
  address: String = "",
  bookImages: BookImages = BookImages.Loaded(emptyList()),
  canUseAddresses: Boolean = true,
  canRemove: Boolean = true,
  preview: CoverPreview? = null,
  eventSink: (CoverUiEvent) -> Unit = {},
) = CoverUiState(
  currentUrl = currentUrl,
  currentSize = currentSize,
  staged = staged,
  mode = mode,
  sources = listOf(
    CoverSource.Best,
    CoverSource.Single(FakeMetadataMatchRepository.Audible),
    CoverSource.Single(FakeMetadataMatchRepository.Google),
  ),
  source = CoverSource.Best,
  title = TextFieldState("Children of Ruin"),
  author = TextFieldState("Adrian Tchaikovsky"),
  results = results,
  sizes = sizes,
  address = TextFieldState(address),
  bookImages = bookImages,
  canUseAddresses = canUseAddresses,
  canRemove = canRemove,
  preview = preview,
  eventSink = eventSink,
)
