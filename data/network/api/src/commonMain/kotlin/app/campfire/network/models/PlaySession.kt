// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * A server-created playback session, the response of `POST /api/items/{id}/play[/{episodeId}]`.
 *
 * Only the fields the client consumes are modeled; the full server payload additionally carries
 * the expanded library item, metadata, chapters, and device info, all ignored on parse.
 */
@Serializable
data class PlaySession(
  val id: String,
  val userId: String? = null,
  val libraryItemId: String,
  val episodeId: String? = null,
  val mediaType: String? = null,
  /** Server-decided play method; values match [app.campfire.core.model.PlayMethod.serverValue]. */
  val playMethod: Int,
  val audioTracks: List<PlaySessionAudioTrack> = emptyList(),
  val duration: Double? = null,
  val currentTime: Double? = null,
  val serverVersion: String? = null,
)

/**
 * One audio track of a [PlaySession]. Deliberately lenient: direct-play sessions return one
 * full record per file, while transcode sessions return a single thin track whose only
 * reliable fields are [contentUrl] (the HLS playlist) and [duration] — most others are null.
 */
@Serializable
data class PlaySessionAudioTrack(
  val contentUrl: String,
  val index: Int? = null,
  val startOffset: Double? = null,
  val duration: Double? = null,
  val title: String? = null,
  val mimeType: String? = null,
  val codec: String? = null,
)
