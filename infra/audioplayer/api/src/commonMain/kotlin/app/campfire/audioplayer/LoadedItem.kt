// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Whether this player has [libraryItemId] loaded and can be controlled, such as seeked, without
 * being prepared first. With an [episodeId], that episode must be the one loaded.
 *
 * Whether the item is the active session isn't enough: after the app is killed, or before the
 * player service connects, the session is still active but nothing is loaded.
 */
fun AudioPlayer.hasLoaded(
  libraryItemId: LibraryItemId,
  episodeId: PodcastEpisodeId? = null,
): Boolean = hasLoaded(libraryItemId, episodeId, state.value)

/** The current player when it [hasLoaded] [libraryItemId], to control it with; otherwise null. */
fun AudioPlayerHolder.loadedPlayerFor(
  libraryItemId: LibraryItemId,
  episodeId: PodcastEpisodeId? = null,
): AudioPlayer? = currentPlayer.value?.takeIf { it.hasLoaded(libraryItemId, episodeId) }

/** Whether the current player [hasLoaded] [libraryItemId], as that changes. */
@OptIn(ExperimentalCoroutinesApi::class)
fun AudioPlayerHolder.observeHasLoaded(
  libraryItemId: LibraryItemId,
  episodeId: PodcastEpisodeId? = null,
): Flow<Boolean> = currentPlayer
  .flatMapLatest { player ->
    // A player sets its session before its state moves, so each state can be checked against it
    player?.state?.map { player.hasLoaded(libraryItemId, episodeId, it) } ?: flowOf(false)
  }
  .distinctUntilChanged()

private fun AudioPlayer.hasLoaded(
  libraryItemId: LibraryItemId,
  episodeId: PodcastEpisodeId?,
  state: AudioPlayer.State,
): Boolean {
  val session = preparedSession ?: return false
  return session.libraryItem.id == libraryItemId &&
    (episodeId == null || session.episodeId == episodeId) &&
    state in ControllableStates
}

/** Disabled and Finished need preparing again, and an Initializing player can't be controlled yet */
private val ControllableStates = setOf(
  AudioPlayer.State.Buffering,
  AudioPlayer.State.Playing,
  AudioPlayer.State.Paused,
)
