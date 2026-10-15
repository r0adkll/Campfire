// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.api

import app.campfire.core.model.LibraryItemId
import app.campfire.settings.api.ReaderLayoutOverrides
import kotlinx.coroutines.flow.Flow

/** Where one book reads differently from the reader's settings for all books, per account. */
interface ReaderBookSettingsRepository {

  fun observeOverrides(itemId: LibraryItemId): Flow<ReaderLayoutOverrides>

  /** Replace [itemId]'s overrides; [ReaderLayoutOverrides.None] removes them. */
  suspend fun setOverrides(itemId: LibraryItemId, overrides: ReaderLayoutOverrides)
}
