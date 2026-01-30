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
