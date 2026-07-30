// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.cache

interface DiskCache<V> : Cache<V> {

  /**
   * Load all cache entries, by key, from the disk
   */
  suspend fun selectAll(): Map<String, V>
}
