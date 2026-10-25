// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.runtime.Immutable
import app.campfire.libraries.api.CoverUpdate

/** A cover to take the place of the book's own once the details are saved, and how to show it. */
@Immutable
data class StagedCover(
  val update: CoverUpdate,
  /** The image to show for it, or null when there's to be no cover */
  val previewUrl: String?,
  /** The file it comes from, for a cover among the book's own files */
  val fileName: String? = null,
) {
  companion object {
    /** A cover for the server to download from [url], as a source offers one */
    fun fromUrl(url: String) = StagedCover(CoverUpdate.FromUrl(url), previewUrl = url)

    /** No cover at all */
    val Removed = StagedCover(CoverUpdate.Remove, previewUrl = null)
  }
}
