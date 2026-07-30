// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.item

import app.campfire.core.model.LibraryItemId
import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.LibraryItemExpanded as NetworkLibraryItem
import org.mobilenativefoundation.store.store5.Fetcher

class LibraryItemFetcherFactory(
  private val api: AudioBookShelfApi,
) {

  fun create(): Fetcher<LibraryItemId, NetworkLibraryItem> {
    return Fetcher.ofResult { itemId: LibraryItemId ->
      api.getLibraryItem(itemId).asFetcherResult()
    }
  }
}
