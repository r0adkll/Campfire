// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.api.match

/** Tells how big a cover is, in pixels, from the start of its file, without loading the rest. */
interface CoverSizeReader {

  /**
   * The size of the image at [url]. A cover on the server is read as the server keeps it, not the
   * resized copy the app shows.
   */
  suspend fun sizeOf(url: String): Result<ImageSize>
}

data class ImageSize(
  val width: Int,
  val height: Int,
) {
  val pixels: Long get() = width.toLong() * height
}
