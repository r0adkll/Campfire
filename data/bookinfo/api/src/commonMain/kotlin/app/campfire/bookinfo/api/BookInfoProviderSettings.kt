// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.api

import kotlinx.coroutines.flow.Flow

/**
 * Per-user enable/disable state for each provider. Providers default to enabled;
 * disabling one removes it from [BookInfoRegistry] selection without unlinking
 * any stored credential.
 */
interface BookInfoProviderSettings {
  fun isEnabled(id: ProviderId): Boolean
  fun setEnabled(id: ProviderId, enabled: Boolean)
  fun observeEnabled(id: ProviderId): Flow<Boolean>

  /**
   * The user's preferred source, or null for the automatic default order.
   * The preferred provider wins whenever it can serve a book; when it can't,
   * selection falls back down the default order.
   */
  fun preferredProvider(): ProviderId?
  fun setPreferredProvider(id: ProviderId?)
  fun observePreferredProvider(): Flow<ProviderId?>
}
