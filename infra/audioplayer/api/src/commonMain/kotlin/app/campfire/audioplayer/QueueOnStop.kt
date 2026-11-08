// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer

/**
 * What happens to the playback queue when a session stops.
 */
enum class QueueOnStop {
  /**
   * When the stopped session is the one playing, the next queued entry starts in its place.
   * Used when the user moves past the current item (marks it finished, resets it, skips it).
   */
  PlayNext,

  /**
   * Playback stops and the queue is left as it is, ready for the next time something plays.
   * Used when the user closes the player or switches accounts.
   */
  Keep,

  /**
   * Playback stops and the queue is emptied.
   */
  Clear,
}
