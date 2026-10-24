// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import kotlinx.serialization.Serializable

@Serializable
data class MediaProgressUpdatePayload(
  val libraryItemId: String? = null,
  val episodeId: String? = null,
  val duration: Float? = null,
  val progress: Float? = null,
  val currentTime: Float? = null,
  val isFinished: Boolean? = null,
  val hideFromContinueListening: Boolean? = null,
  val finishedAt: Long? = null,
  /** Ignored by the server, which takes a progress's start from [createdAt] instead. */
  val startedAt: Long? = null,
  /** When the progress started, honored only when this update creates the progress. */
  val createdAt: Long? = null,
  val ebookLocation: String? = null,
  val ebookProgress: Float? = null,
)
