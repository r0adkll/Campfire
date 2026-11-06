// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisodeId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Where the item this device has prepared is right now, according to its player.
 *
 * The session and progress rows hold a saved copy of the position that trails the player while it
 * plays, so anything that shows the position as it moves reads it from here instead.
 */
data class LivePosition(
  val libraryItemId: LibraryItemId,
  /** Null for a book. */
  val episodeId: PodcastEpisodeId?,
  /** Into the book, or into the episode for a podcast, in whole seconds. */
  val time: Duration,
)

/**
 * The prepared item's [LivePosition], changing at most once a second, or null while there is no
 * player or it has no position of its own yet (before it is prepared).
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun AudioPlayerHolder.observeLivePosition(): Flow<LivePosition?> =
  currentPlayer
    .flatMapLatest { player ->
      if (player == null) {
        flowOf(null)
      } else {
        combine(player.state, player.overallTime) { state, time ->
          val session = player.preparedSession
          when {
            session == null -> null
            state == AudioPlayer.State.Disabled || state == AudioPlayer.State.Initializing -> null
            else -> LivePosition(
              libraryItemId = session.libraryItem.id,
              episodeId = session.episodeId,
              time = time.inWholeSeconds.seconds,
            )
          }
        }
      }
    }
    .distinctUntilChanged()

/**
 * This progress with its position replaced by [live] when [live] is for the same book or episode,
 * so a progress readout keeps up with the player between saves. Finished progress is left alone.
 */
fun MediaProgress.withLivePosition(live: LivePosition?): MediaProgress {
  if (live == null || isFinished) return this
  // Book progress stores its episode as null or the empty-string sentinel
  if (libraryItemId != live.libraryItemId || episodeId?.ifEmpty { null } != live.episodeId) return this

  val seconds = live.time.inWholeMilliseconds / 1000f
  return copy(
    currentTime = seconds,
    progress = duration?.takeIf { it > 0f }?.let { (seconds / it).coerceIn(0f, 1f) } ?: progress,
  )
}
