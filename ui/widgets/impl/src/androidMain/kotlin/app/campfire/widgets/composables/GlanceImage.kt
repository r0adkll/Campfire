// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets.composables

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import coil3.imageLoader
import coil3.memory.MemoryCache
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Now that Campfire will dynamically request images @ size
 * the widgets could end up trying to render very large images
 * that break them. So if not specified, let's cap widget image sizes.
 */
private const val DEFAULT_IMAGE_SIZE = 400

@Composable
internal fun GlanceImage(
  url: Any?,
  modifier: GlanceModifier = GlanceModifier,
  size: Size? = null,
  contentScale: ContentScale = ContentScale.Crop,
  colorFilter: ColorFilter? = null,
) {
  val context = LocalContext.current
  val requestSize = size ?: Size(DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE)
  val cacheKey = remember(url, requestSize) { url?.let { widgetImageCacheKey(it, requestSize) } }

  // Start from the memory cache when the image is already in it. Every size the widget is shown
  // at composes its own copy of this, and a new widget session composes everything again; each
  // image that arrives later changes the tree, and every change re-sends the whole widget.
  var bitmap by remember(cacheKey) {
    mutableStateOf(cacheKey?.let { context.imageLoader.memoryCache?.get(it)?.image?.toBitmap() })
  }

  LaunchedEffect(cacheKey) {
    if (bitmap != null) return@LaunchedEffect
    withContext(Dispatchers.IO) {
      val request = ImageRequest.Builder(context)
        .data(url)
        .size(requestSize)
        .memoryCacheKey(cacheKey)
        .build()

      bitmap = when (val result = context.imageLoader.execute(request)) {
        is ErrorResult -> null
        is SuccessResult -> {
          result.image.toBitmap()
        }
      }
    }
  }

  bitmap.let {
    if (bitmap != null) {
      Image(
        provider = ImageProvider(bitmap!!),
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = colorFilter,
      )
    } else {
      Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator()
      }
    }
  }
}

/** Widget renditions are cached apart from the app's own, which request other sizes. */
private fun widgetImageCacheKey(url: Any, size: Size): MemoryCache.Key =
  MemoryCache.Key("widget:$url@$size")

@Composable
internal fun GlanceModifier.imageBackground(
  url: Any?,
  contentScale: ContentScale = ContentScale.Crop,
  colorFilter: ColorFilter? = null,
): GlanceModifier {
  val context = LocalContext.current
  var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }

  LaunchedEffect(url) {
    withContext(Dispatchers.IO) {
      val request = ImageRequest.Builder(context)
        .data(url)
        .build()

      bitmap = when (val result = context.imageLoader.execute(request)) {
        is ErrorResult -> null
        is SuccessResult -> {
          result.image.toBitmap()
        }
      }
    }
  }

  return bitmap.let {
    if (bitmap != null) {
      this.background(
        imageProvider = ImageProvider(bitmap!!),
        contentScale = contentScale,
        colorFilter = colorFilter,
      )
    } else {
      this
    }
  }
}
