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
}
