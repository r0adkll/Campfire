// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.isSpecified
import app.campfire.core.image.CoverUrls
import coil3.SingletonImageLoader
import coil3.compose.DrawScopeSizeResolver
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
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
 *
 * A known [size] is rounded up to the [CoverUrls.WIDTH_BUCKETS] rendition the server would hand back
 * for it. `CoverSizingInterceptor` puts that width in the URL and the URL *is* the memory cache key,
 * so bucketing keeps both the request and its cache entry still while a slot resizes by a few pixels
 * (a drag, a window resize, a `weight`/`aspectRatio` reflow) instead of decoding it over and over.
 */
@Composable
fun rememberDrawSizedRequest(model: Any?, size: Dp = Dp.Unspecified): ImageRequest {
  val context = LocalPlatformContext.current
  val memoryCache = SingletonImageLoader.get(context).memoryCache
  val sizePx = if (size.isSpecified && size.isFinite) {
    with(LocalDensity.current) { size.roundToPx() }
      .takeIf { it > 0 }
      ?.let(CoverUrls::bucketWidth)
  } else {
    null
  }
  return remember(context, model, sizePx) {
    ImageRequest.Builder(context)
      .data(model)
      .apply {
        if (sizePx != null && sizePx > 0) {
          size(Size(sizePx, sizePx))
          memoryCache?.smallerCachedRendition(model, sizePx)?.let(::placeholderMemoryCacheKey)
        } else {
          size(DrawScopeSizeResolver())
        }
      }
      .build()
  }
}

/**
 * The key of the largest already-resident rendition of [model] narrower than [widthPx], if any.
 *
 * Handed to a request as its `placeholderMemoryCacheKey`, this is what keeps a shared-element
 * transition from flying a spinner into place: opening an item detail asks for a wider rendition than
 * the card that launched it, which is a different URL and so a cold cache entry. Seeding the request
 * with the rendition the card already loaded means the cover is on screen for the whole transition
 * and only sharpens once the larger one arrives.
 *
 * Only Audiobookshelf cover/author URLs have sibling renditions to find — everything else is served
 * at one size under one key.
 */
internal fun MemoryCache.smallerCachedRendition(model: Any?, widthPx: Int): MemoryCache.Key? {
  val url = model as? String ?: return null
  if (!CoverUrls.isServerImageUrl(url)) return null
  return CoverUrls.WIDTH_BUCKETS
    .asReversed()
    .asSequence()
    .filter { it < widthPx }
    .map { MemoryCache.Key(CoverUrls.sized(url, it)) }
    .firstOrNull { this[it] != null }
}
