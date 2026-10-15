// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.libraries.api.PurgedItemCleanup
import app.campfire.reader.api.EbookRepository
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

/**
 * Removes a purged item's ebook, downloaded or cached. The repository is lazy: it reads items
 * through the library repository, which purges through the purger that holds this.
 */
@ContributesIntoSet(UserScope::class, binding = binding<PurgedItemCleanup>())
@Inject
class EbookFilesCleanup(
  private val ebookRepository: Lazy<EbookRepository>,
) : PurgedItemCleanup {
  override suspend fun onPurged(itemId: LibraryItemId) = ebookRepository.value.deleteFiles(itemId)
}
