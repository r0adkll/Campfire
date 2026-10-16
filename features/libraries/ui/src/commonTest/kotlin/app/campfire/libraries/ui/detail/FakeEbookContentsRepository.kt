// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail

import app.campfire.core.model.LibraryItemId
import app.campfire.reader.api.EbookContentsEntry
import app.campfire.reader.api.EbookContentsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeEbookContentsRepository : EbookContentsRepository {

  val contents = MutableStateFlow(emptyList<EbookContentsEntry>())

  override fun observeContents(itemId: LibraryItemId): Flow<List<EbookContentsEntry>> = contents

  override suspend fun saveContents(itemId: LibraryItemId, entries: List<EbookContentsEntry>) {
    contents.value = entries
  }
}
