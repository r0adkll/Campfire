// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.sync

import app.campfire.core.model.LibraryItemId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Decides when the playing position is worth saving to the session row.
 *
 * The player reports its position every 500 ms, and every save re-runs every query that reads the
 * session or progress tables, so saving each one costs far more than it is worth. Anything showing the
 * position as it moves reads it from the player (`AudioPlayerHolder.observeLivePosition()`), so the
 * saved copy only has to be close enough to resume from and sync. While playing it is saved at most
 * every [SaveInterval] of wall-clock time, or straight away when the position jumps by [JumpThreshold]
 * (a seek, or fast playback). Outside playback every change is saved, and [flush] saves whatever is
 * still unsaved when playback stops.
 */
internal class PositionSaveThrottle(
  private val nowMillis: () -> Long,
) {

  private class Position(val libraryItemId: LibraryItemId, val time: Duration)

  private val mutex = Mutex()
  private var isPlaying = false
  private var latest: Position? = null
  private var saved: Position? = null
  private var savedAtMillis = 0L

  /** Records the playing state; positions reported outside playback are saved as they come. */
  suspend fun onPlayingChanged(playing: Boolean) = mutex.withLock {
    isPlaying = playing
  }

  /** Records [time] for [libraryItemId], returning true when the caller should save it now. */
  suspend fun onPosition(libraryItemId: LibraryItemId, time: Duration): Boolean = mutex.withLock {
    val position = Position(libraryItemId, time)
    latest = position

    val last = saved?.takeIf { it.libraryItemId == libraryItemId }
    val shouldSave = when {
      last == null || !isPlaying -> true
      (time - last.time).absoluteValue >= JumpThreshold -> true
      else -> nowMillis() - savedAtMillis >= SaveInterval.inWholeMilliseconds
    }
    if (shouldSave) markSaved(position)
    shouldSave
  }

  /**
   * The position of [libraryItemId] that was reported but not yet saved, if any, marking it saved.
   * For when playback stops, so the session holds exactly where the user left off.
   */
  suspend fun flush(libraryItemId: LibraryItemId): Duration? = mutex.withLock {
    val pending = latest?.takeIf { it.libraryItemId == libraryItemId } ?: return@withLock null
    if (saved?.libraryItemId == libraryItemId && saved?.time == pending.time) return@withLock null
    markSaved(pending)
    pending.time
  }

  private fun markSaved(position: Position) {
    saved = position
    savedAtMillis = nowMillis()
  }

  companion object {
    /** The longest a playing position goes unsaved, and how stale a resume after a crash can be. */
    val SaveInterval: Duration = 5.seconds

    /** A position change at least this large is saved right away. */
    val JumpThreshold: Duration = 15.seconds
  }
}
