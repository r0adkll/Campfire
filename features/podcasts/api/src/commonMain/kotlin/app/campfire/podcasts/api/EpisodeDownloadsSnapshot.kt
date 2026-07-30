// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.api

/**
 * A point-in-time snapshot of the server's podcast download state for one library,
 * as returned by `GET /api/libraries/{id}/episode-downloads`.
 *
 * Used to hydrate [RemoteEpisodeDownloadTracker] at startup before the socket has emitted
 * any events. Once the socket is live, incremental updates take over.
 */
data class EpisodeDownloadsSnapshot(
  /** The download the server is actively processing right now, if any. */
  val currentDownload: RemoteEpisodeDownload?,

  /** Downloads waiting in the queue, in server order. */
  val queue: List<RemoteEpisodeDownload>,
)
