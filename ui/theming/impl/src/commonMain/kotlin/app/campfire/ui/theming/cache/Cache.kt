// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.cache

/**
 * A generic cache interface for storing theming related information
 */
interface Cache<V> {
  suspend operator fun get(key: String): V?
  suspend operator fun set(key: String, value: V)
  suspend fun putAll(from: Map<String, V>)
  suspend fun remove(key: String)
  suspend fun containsKey(key: String): Boolean
}
