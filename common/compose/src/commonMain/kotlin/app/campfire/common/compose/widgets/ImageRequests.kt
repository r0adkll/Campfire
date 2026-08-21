// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.compose.DrawScopeSizeResolver
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest

/**
 * An [ImageRequest] for [model] whose target size is resolved from the painter's draw bounds.
 *
 * `rememberAsyncImagePainter` falls back to `Size.ORIGINAL` when a request has no size resolver,
 * which hides the displayed size from the image loader and makes every cover request the largest
 * rendition. Use this for painter-based images so the server is asked for an appropriately sized
 * cover instead.
 */
@Composable
fun rememberDrawSizedRequest(model: Any?): ImageRequest {
  val context = LocalPlatformContext.current
  return remember(context, model) {
    ImageRequest.Builder(context)
      .data(model)
      .size(DrawScopeSizeResolver())
      .build()
  }
}
