// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import app.campfire.common.test.mediaProgress
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata

/** A book from the user's library, with a cover URL tests can resolve to a color. */
internal fun previewBook(
  id: String,
  title: String,
  author: String,
  progress: Float? = null,
): PreviewShelfItem = PreviewShelfItem(
  libraryItem(
    id = id,
    media = media(
      metadata = mediaMetadata(title = title, subtitle = null, authorName = author),
      coverImageUrl = "https://abs.example.net/api/items/$id/cover",
    ),
    userMediaProgress = progress?.let {
      mediaProgress(libraryItemId = id, duration = 36_000f, currentTime = 36_000f * it, progress = it)
    },
  ),
)

/** A library the way the user's own shelves come through. */
internal fun previewLibrary(
  itemCount: Int? = 65,
  isPodcastLibrary: Boolean = false,
) = PreviewLibrary(
  continueListening = PreviewShelf(
    label = "Continue Listening",
    items = listOf(
      previewBook("li_hail_mary", "Project Hail Mary", "Andy Weir", progress = 0.42f),
      previewBook("li_piranesi", "Piranesi", "Susanna Clarke", progress = 0.73f),
    ),
  ),
  recentlyAdded = PreviewShelf(
    label = "Recently Added",
    items = listOf(
      previewBook("li_circe", "Circe", "Madeline Miller"),
      previewBook("li_dune", "Dune", "Frank Herbert"),
      previewBook("li_hobbit", "The Hobbit", "J. R. R. Tolkien"),
    ),
  ),
  itemCount = itemCount,
  isPodcastLibrary = isPodcastLibrary,
)
