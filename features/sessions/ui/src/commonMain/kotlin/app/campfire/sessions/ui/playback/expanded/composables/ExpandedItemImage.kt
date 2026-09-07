// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ShakeVeryHigh
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.CoverImageShape
import app.campfire.core.model.Session
import app.campfire.sessions.ui.SharedImage
import app.campfire.sessions.ui.composables.RunningTimerText

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.ExpandedItemImage(
  session: Session?,
  currentMetadata: Metadata,
  runningTimer: RunningTimer?,
  animatedVisibilityScope: AnimatedVisibilityScope,
  size: Dp,
  modifier: Modifier = Modifier,
  shape: Shape = CoverImageShape,
) {
  BoxWithConstraints(
    contentAlignment = Alignment.Center,
    modifier = modifier,
  ) {
    val mediaUrl = currentMetadata.artworkUri
      ?: session?.libraryItem?.media?.coverImageUrl
    // The cover fills a weight/aspectRatio slot, so its layout size is Dp.Unspecified. Request the
    // rendition at the measured slot width instead, otherwise the shared-element transition latches
    // the draw-bounds resolver onto the tiny mini-bar bounds and the full-screen cover renders blurry.
    //
    // Dragging the expanded sheet insets it a little more on every frame, which re-measures this slot
    // continuously; feeding that straight into the request would rebuild it — and the cache key derived
    // from it — dozens of times per gesture. Latch the widest slot seen so the view sticks to a single
    // rendition for its lifetime.
    val widthLatch = remember { SlotWidthLatch() }
    val coverRequestSize = if (size != Dp.Unspecified) size else widthLatch.widen(maxWidth)
    CoverImage(
      imageUrl = mediaUrl,
      contentDescription = session?.libraryItem?.media?.metadata?.title,
      size = size,
      requestSize = coverRequestSize,
      shape = shape,
      sharedElementModifier = Modifier.fillMaxSize(),
      modifier = Modifier
        .fillMaxSize()
        .sharedElement(
          rememberSharedContentState(SharedImage),
          animatedVisibilityScope = animatedVisibilityScope,
        ),
    )

    // Callers size the cover through the slot (size == Dp.Unspecified), so the scrim has to match
    // the parent rather than request [size] itself, and it clips to the same [shape] as the cover.
    AnimatedVisibility(
      visible = runningTimer != null,
      enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
      modifier = Modifier.matchParentSize(),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(0.3f), shape),
      ) {
        if (runningTimer?.isShakeToRestartEnabled == true) {
          Icon(
            CampfireIcons.Rounded.ShakeVeryHigh,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(16.dp),
          )
        }

        if (runningTimer != null) {
          RunningTimerText(
            runningTimer = runningTimer,
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
          )
        }
      }
    }
  }
}

/**
 * Remembers the widest slot a cover has been measured into.
 *
 * Deliberately not snapshot state: [BoxWithConstraints] already re-runs its content whenever the
 * measured width changes, so a wider slot is picked up by that same pass without an invalidation —
 * and without the backwards-write recomposition loop a [androidx.compose.runtime.MutableState] would
 * cause here. Widths only ever grow, so shrinking the slot (a drag, a window resize) reuses the
 * rendition already in memory instead of fetching a smaller one.
 */
private class SlotWidthLatch {
  private var width: Dp = 0.dp

  fun widen(candidate: Dp): Dp {
    if (candidate.isFinite && candidate > width) width = candidate
    return width
  }
}
