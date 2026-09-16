// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.api

import app.campfire.core.model.LibraryItemId

/**
 * Removes library items the server no longer has from the local cache: the item, its media,
 * every list/series/collection/playlist/search reference to it, and its offline downloads.
 *
 * An item is only ever purged on confirmation that it's gone — a socket `item_removed` event
 * or a 404 for that id. Being absent from a listing isn't enough on its own, since listings
 * are filtered by the user's permissions (the server answers 403, not 404, for those).
 */
interface LibraryItemPurger {

  /**
   * Purge [itemIds], which are already confirmed removed from the server.
   */
  suspend fun purge(itemIds: Collection<LibraryItemId>)

  /**
   * Ask the server about each of [candidates] and purge the ones it reports as not found.
   * Items that can't be checked (offline, 403, server error) are left alone.
   * @return the ids that were purged
   */
  suspend fun purgeIfRemoved(candidates: Collection<LibraryItemId>): Set<LibraryItemId>
}
