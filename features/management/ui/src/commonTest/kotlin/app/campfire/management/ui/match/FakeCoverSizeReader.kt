// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.match

import app.campfire.management.api.match.CoverSizeReader
import app.campfire.management.api.match.ImageSize

class FakeCoverSizeReader : CoverSizeReader {

  /** The sizes to answer with, by address; any other cover has none */
  val sizes = mutableMapOf<String, ImageSize>()
  val reads = mutableListOf<String>()

  override suspend fun sizeOf(url: String): Result<ImageSize> {
    reads += url
    return sizes[url]?.let { Result.success(it) } ?: Result.failure(IllegalStateException("No size for $url"))
  }
}
