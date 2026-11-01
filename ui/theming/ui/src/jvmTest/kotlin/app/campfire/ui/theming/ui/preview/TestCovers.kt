// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil3.ColorImage
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult

/**
 * Loads every cover as a flat, muted color picked from its URL, standing in for cover art in
 * render tests. Undo it with [resetTestCovers].
 */
internal fun installTestCovers() {
  SingletonImageLoader.reset()
  SingletonImageLoader.setUnsafe(
    ImageLoader.Builder(PlatformContext.INSTANCE)
      .components { add(CoverInterceptor) }
      .build(),
  )
}

/** The flat color the test image loader draws the cover at [url] in. */
internal fun testCoverColor(url: String): Color {
  val hue = (url.hashCode() and 0xFFFF) % 360
  return Color.hsl(hue.toFloat(), saturation = 0.45f, lightness = 0.42f)
}

internal fun resetTestCovers() {
  SingletonImageLoader.reset()
}

private object CoverInterceptor : Interceptor {
  override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
    val color = testCoverColor(chain.request.data.toString())
    return SuccessResult(
      image = ColorImage(color.toArgb(), width = 400, height = 400),
      request = chain.request,
      dataSource = DataSource.MEMORY,
    )
  }
}
