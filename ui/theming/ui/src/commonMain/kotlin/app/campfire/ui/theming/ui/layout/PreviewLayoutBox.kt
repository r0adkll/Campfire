// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.toSize
import app.campfire.common.compose.session.LocalPlayerDocked

/**
 * Lays out [content] with the [PreviewLayout] for the space inside [contentPadding], divided by
 * any fold that crosses it. The box itself fills the space, padding included, so [content] can
 * scroll under the bars the padding makes room for.
 */
@Composable
internal fun PreviewLayoutBox(
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
  content: @Composable (PreviewLayout) -> Unit,
) {
  // Not boundsInWindow, which clips to the window and so would misjudge the width.
  var bounds by remember { mutableStateOf(Rect.Zero) }
  val posture = currentWindowAdaptiveInfoV2().windowPosture
  val density = LocalDensity.current
  val layoutDirection = LocalLayoutDirection.current
  val playerDocked = LocalPlayerDocked.current
  val start = contentPadding.calculateStartPadding(layoutDirection)
  val end = contentPadding.calculateEndPadding(layoutDirection)
  val top = contentPadding.calculateTopPadding()
  val bottom = contentPadding.calculateBottomPadding()
  val contentBounds = if (bounds.isEmpty) {
    Rect.Zero
  } else {
    with(density) {
      Rect(
        left = bounds.left + start.toPx(),
        top = bounds.top + top.toPx(),
        right = bounds.right - end.toPx(),
        bottom = bounds.bottom - bottom.toPx(),
      )
    }
  }
  val hinge = remember(posture, contentBounds, density, playerDocked) {
    foldHinge(posture, contentBounds, density, playerDocked)
  }
  BoxWithConstraints(modifier.onPlaced { bounds = Rect(it.positionInWindow(), it.size.toSize()) }) {
    val size = DpSize(maxWidth - start - end, maxHeight - top - bottom)
    content(previewLayout(size, hinge))
  }
}
