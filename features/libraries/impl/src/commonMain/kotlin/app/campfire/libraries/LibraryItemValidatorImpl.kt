// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries

import app.campfire.core.di.AppScope
import app.campfire.core.extensions.seconds
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.api.LibraryItemValidation
import app.campfire.libraries.api.LibraryItemValidator
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.math.floor
import kotlin.time.Duration.Companion.seconds
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class LibraryItemValidatorImpl : LibraryItemValidator {

  override fun validate(item: LibraryItem): LibraryItemValidation {
    // Validate that the item is not minified
    if (item.media.tracks.isEmpty()) return LibraryItemValidation.Error.NoTracks
    if (item.media.chapters.isEmpty()) return LibraryItemValidation.Error.NoChapters

    // compute total duration of all tracks so to compare chapters against it
    // Floor the value so we don't get snagged by rounding errors
    val totalDuration = floor(
      item.media.tracks.sumOf { it.duration.toDouble() },
    ).seconds

    // Check if any chapters exist outside the actual duration of tracks
    val invalidChapterIds = mutableSetOf<Int>()
    for (chapter in item.media.chapters) {
      if (
        chapter.start.seconds > totalDuration ||
        floor(chapter.end).seconds > totalDuration ||
        chapter.start < 0f
      ) {
        invalidChapterIds += chapter.id
      }
    }

    return if (invalidChapterIds.isNotEmpty()) {
      LibraryItemValidation.Error.InvalidChapters(invalidChapterIds)
    } else {
      LibraryItemValidation.Success
    }
  }
}
