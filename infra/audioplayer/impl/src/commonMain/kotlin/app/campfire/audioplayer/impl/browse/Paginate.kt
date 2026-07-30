// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.browse

/**
 * Slice a fully-loaded child list to the page a media browser requested.
 *
 * [page] is zero-based. A page past the end returns an empty list (how browsers detect
 * the end of pagination). A non-positive [pageSize] is treated as "no paging" and
 * returns the full list — defensive, since Media3 requires pageSize >= 1.
 */
internal fun <T> List<T>.paginate(page: Int, pageSize: Int): List<T> {
  if (pageSize <= 0) return this
  if (page < 0) return emptyList()

  // Long math: page * pageSize overflows Int for large pageSize values (e.g. a browser
  // paging with pageSize = Int.MAX_VALUE).
  val fromIndex = page.toLong() * pageSize
  if (fromIndex >= size) return emptyList()

  val toIndex = minOf(size.toLong(), fromIndex + pageSize)
  return subList(fromIndex.toInt(), toIndex.toInt())
}
