// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import app.campfire.core.extensions.seconds
import kotlin.time.Duration

typealias MediaProgressId = String

data class MediaProgress(
  val id: MediaProgressId,
  val userId: String,
  val libraryItemId: String,
  val episodeId: String? = null,
  val mediaItemId: String,
  val mediaItemType: MediaType,
  val duration: Float?,
  val progress: Float,
  val currentTime: Float,
  val isFinished: Boolean,
  val hideFromContinueListening: Boolean,
  val ebookLocation: String? = null,
  val ebookProgress: Float? = null,
  val lastUpdate: Long,
  val startedAt: Long,
  val finishedAt: Long? = null,
  val source: Source,
) {

  /**
   * Describes where this media progress was written from
   */
  enum class Source {
    /**
     * The media progress was created locally
     */
    Local,

    /**
     * The media progress was written from the server
     */
    Remote,
  }

  val isValid: Boolean
    get() = progress > 0f || (duration ?: 0f) > 0f

  /**
   * Get the [currentTime] in [Duration] units, accounting for [isFinished],
   * where if true, it returns 0 duration.
   */
  val actualTime: Duration
    get() = if (isFinished) Duration.ZERO else currentTime.seconds

  val actualProgress: Float
    get() = if (isFinished) {
      1f
    } else duration?.let {
      currentTime / duration
    } ?: progress

  companion object {
    const val UNKNOWN_ID = "unknown_id"
  }
}
