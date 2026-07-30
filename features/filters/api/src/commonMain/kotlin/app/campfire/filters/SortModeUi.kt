// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.filters

import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortModeConfig
import com.slack.circuit.overlay.OverlayHost

interface SortModeUi {

  suspend fun showContentSortModeBottomSheet(
    overlayHost: OverlayHost,
    current: ContentSortMode,
    currentDirection: SortDirection,
    config: SortModeConfig,
  ): ContentSortMode?
}
