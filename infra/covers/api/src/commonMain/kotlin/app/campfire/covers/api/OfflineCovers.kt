// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.covers.api

import app.campfire.core.image.CoverUrls
import app.campfire.core.model.LibraryItemId

/**
 * Cover artwork kept on this device for items downloaded to listen to or read offline, so they
 * show without the server, and after the OS clears the image cache.
 */
interface OfflineCovers {

  /** [itemId]'s kept cover, or null when it has none. */
  suspend fun get(itemId: LibraryItemId): OfflineCover?
}

data class OfflineCover(
  /** Absolute path of the image on this device. */
  val path: String,
  /** The item's last update when the cover was kept, the `ts` of its cover URL, or null when unknown. */
  val timestamp: Long?,
) {

  /**
   * Whether this is the cover [url] asks for, so it can stand in without asking the server. A URL
   * without a `ts` can't tell, and takes the kept one, which is refreshed whenever the item changes.
   */
  fun isCurrentFor(url: String): Boolean {
    val wanted = CoverUrls.timestamp(url) ?: return true
    return wanted == timestamp
  }
}
