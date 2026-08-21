// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.image

/**
 * Helpers for Audiobookshelf cover/author image URLs.
 *
 * The server never serves the original file for `/api/items/{id}/cover` or `/api/authors/{id}/image`;
 * it resizes to the requested `width`/`height` (defaulting to 400px wide) and caches that rendition
 * on disk. Asking for a size that matches where the image is displayed keeps thumbnails cheap and
 * full-screen artwork sharp.
 */
object CoverUrls {

  /** Widths the server is asked for, smallest to largest. Requests are rounded up to the next bucket. */
  val WIDTH_BUCKETS: List<Int> = listOf(200, 300, 400, 600, 800, 1200, 1600)

  /** Width used when the display size is unknown (e.g. media notifications, widgets, lock screens). */
  const val ARTWORK_WIDTH = 1200

  private val SERVER_IMAGE_PATH = Regex("""/api/(items/[^/?#]+/cover|authors/[^/?#]+/image)(?:[?#].*)?$""")
  private val SIZE_PARAMS = setOf("width", "height", "raw")

  /** True when [url] points at a server-resized cover or author image endpoint. */
  fun isServerImageUrl(url: String): Boolean = SERVER_IMAGE_PATH.containsMatchIn(url)

  /** Rounds [widthPx] up to the nearest [WIDTH_BUCKETS] entry (capped at the largest); null when unknown. */
  fun bucketWidth(widthPx: Int?): Int {
    if (widthPx == null || widthPx <= 0) return ARTWORK_WIDTH
    return WIDTH_BUCKETS.firstOrNull { it >= widthPx } ?: WIDTH_BUCKETS.last()
  }

  /**
   * Returns [url] with a `width` query parameter sized for [widthPx], or [url] unchanged when it is not a
   * server image URL or already specifies a size.
   */
  fun sized(url: String, widthPx: Int?): String {
    if (!isServerImageUrl(url) || hasSizeParam(url)) return url
    val width = bucketWidth(widthPx)
    val fragmentIndex = url.indexOf('#')
    val base = if (fragmentIndex >= 0) url.substring(0, fragmentIndex) else url
    val fragment = if (fragmentIndex >= 0) url.substring(fragmentIndex) else ""
    val separator = if ('?' in base) "&" else "?"
    return "$base${separator}width=$width$fragment"
  }

  private fun hasSizeParam(url: String): Boolean {
    val queryStart = url.indexOf('?')
    if (queryStart < 0) return false
    val query = url.substring(queryStart + 1).substringBefore('#')
    return query.split('&').any { it.substringBefore('=') in SIZE_PARAMS }
  }
}
