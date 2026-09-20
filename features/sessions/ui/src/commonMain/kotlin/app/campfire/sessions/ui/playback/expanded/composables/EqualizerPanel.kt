// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.back.OverlayPriorityBackHandler
import app.campfire.core.model.LibraryItemId
import app.campfire.sessions.ui.sheets.equalizer.EqualizerContent
import app.campfire.sessions.ui.sheets.equalizer.EqualizerInput

/**
 * Swaps the player's cover and transport for the equalizer, leaving the tool column beside them
 * in place.
 *
 * The equalizer is the one tool a side panel serves badly: ten vertical band sliders want height,
 * and a panel down the edge of an already short region has less of it than the region does. Taking
 * the body instead gives it the full height, and leaves the button that opened it on screen to
 * close it again — which is why this is a swap rather than something drawn over the top.
 */
@Composable
internal fun EqualizerPanelSwitcher(
  open: Boolean,
  itemId: LibraryItemId?,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  // Back closes the equalizer before it closes the player, the same order the sheet gave.
  OverlayPriorityBackHandler(enabled = open, onBack = onClose)

  AnimatedContent(
    targetState = open && itemId != null,
    modifier = modifier,
  ) { showEqualizer ->
    if (showEqualizer && itemId != null) {
      EqualizerContent(
        input = EqualizerInput(itemId),
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      content()
    }
  }
}
