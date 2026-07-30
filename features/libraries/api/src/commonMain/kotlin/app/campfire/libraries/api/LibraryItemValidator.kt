// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.api

import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItem

/**
 * Will validate a library item's [app.campfire.core.model.Chapter] and
 * [app.campfire.core.model.AudioTrack] to ensure there is a contiguous
 * alignment of chapter metadata to the actual audio itself. If there
 * is mis-alignment then this is a sign that the Chapter information needs
 * to be corrected on the server.
 *
 * In this scenario the MediaItems generated and fed to the AudioPlayer may
 * not be the most optimal scenario. Actions like skip next/previous may jump
 * MUCH farther than anticipated
 */
interface LibraryItemValidator {

  fun validate(item: LibraryItem): LibraryItemValidation
}

sealed interface LibraryItemValidation {
  /**
   * This [LibraryItem] as a proper [app.campfire.core.model.Chapter] to [app.campfire.analytics.events.AudioTrack]
   * alignment and will process as expected in the AudioPlayer
   */
  data object Success : LibraryItemValidation

  sealed interface Error : LibraryItemValidation {
    data object NoTracks : Error
    data object NoChapters : Error

    data class InvalidChapters(
      val chapterIds: Set<Int>,
    ) : Error
  }
}
