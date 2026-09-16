// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.root.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass

/**
 * How the window divides when a foldable is half-open on a table: content above the hinge, the
 * player below it.
 *
 * [topSizeClass] is the size class of the upper region alone. On every current foldable that
 * region is wide and short, so it computes to the landscape-phone class and the content lays out
 * exactly as it does on a phone held sideways.
 */
@Immutable
internal data class TabletopSplit(
  /** Height of the region above the hinge. */
  val topHeight: Dp,
  /** The hinge itself — a gap neither region draws into. */
  val hingeHeight: Dp,
  val topSizeClass: WindowSizeClass,
)

/**
 * The split for a window of [windowSize] pixels in [posture], or null when the device is not in
 * tabletop posture. Posture reports the hinge in window pixels; a tabletop posture with no
 * horizontal hinge on record (which the platform should never produce) splits the window in half.
 */
internal fun computeTabletopSplit(
  posture: Posture,
  windowSize: IntSize,
  density: Density,
): TabletopSplit? {
  if (!posture.isTabletop) return null
  if (windowSize.width <= 0 || windowSize.height <= 0) return null

  val windowHeight = windowSize.height.toFloat()
  val hinge = posture.hingeList.firstOrNull { !it.isVertical }
  val topPx = (hinge?.bounds?.top ?: (windowHeight / 2f)).coerceIn(0f, windowHeight)
  val bottomPx = (hinge?.bounds?.bottom ?: topPx).coerceIn(topPx, windowHeight)

  with(density) {
    val topHeight = topPx.toDp()
    val bottomHeight = (windowHeight - bottomPx).toDp()

    // Hinge bounds that leave either side a sliver — which the platform should never report —
    // would dock a player with nothing to draw in. Better no split than a broken one.
    if (topHeight < MinSplitRegionHeight || bottomHeight < MinSplitRegionHeight) return null

    val width = windowSize.width.toDp()
    return TabletopSplit(
      topHeight = topHeight,
      hingeHeight = (bottomPx - topPx).toDp(),
      topSizeClass = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(width.value, topHeight.value),
    )
  }
}

/**
 * The least either region can be and still hold anything: comfortably under the ~340dp a real
 * foldable's half comes to, so only nonsense hinge bounds fall below it.
 */
private val MinSplitRegionHeight = 240.dp

/** The current window's [TabletopSplit], or null outside tabletop posture. */
@Composable
internal fun rememberTabletopSplit(): TabletopSplit? {
  val posture = currentWindowAdaptiveInfoV2().windowPosture
  val windowSize = LocalWindowInfo.current.containerSize
  val density = LocalDensity.current
  return remember(posture, windowSize, density) {
    computeTabletopSplit(posture, windowSize, density)
  }
}

/**
 * Lays [content] above the hinge and [bottomContent] below it. Each region only sees the insets
 * on its own edge of the screen: the top keeps the status bar and gives up everything at the
 * bottom, the bottom the reverse. A camera cutout on the side of the display — which is where
 * it lands with the hinge horizontal — is padded out of both regions here, since the screens
 * inside only reserve room for the system bars.
 */
@Composable
internal fun TabletopSplitLayout(
  split: TabletopSplit,
  bottomContent: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val sideCutout = WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
  Column(modifier.fillMaxSize()) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(split.topHeight)
        .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        .windowInsetsPadding(sideCutout),
    ) {
      content()
    }

    Spacer(Modifier.height(split.hingeHeight))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
        .windowInsetsPadding(sideCutout),
    ) {
      bottomContent()
    }
  }
}
