// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import coil3.compose.DrawScopeSizeResolver
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Size

/**
 * An [ImageRequest] for [model] sized for where it will be displayed.
 *
 * `rememberAsyncImagePainter` falls back to `Size.ORIGINAL` when a request has no size resolver,
 * which hides the displayed size from the image loader and makes every cover request the largest
 * rendition. When [size] is known it is used directly (in pixels); otherwise the size is resolved
 * from the painter's draw bounds.
 *
 * Prefer passing [size] for images that take part in a shared element transition: the draw-bounds
 * resolver latches onto the *first* draw, which during a transition is the origin element's size,
 * so a full-screen cover would otherwise be fetched and decoded at thumbnail size.
 */
@Composable
fun rememberDrawSizedRequest(model: Any?, size: Dp = Dp.Unspecified): ImageRequest {
  val context = LocalPlatformContext.current
  val sizePx = if (size.isSpecified) with(LocalDensity.current) { size.roundToPx() } else null
  return remember(context, model, sizePx) {
    ImageRequest.Builder(context)
      .data(model)
      .apply {
        if (sizePx != null && sizePx > 0) size(Size(sizePx, sizePx)) else size(DrawScopeSizeResolver())
      }
      .build()
  }
}
