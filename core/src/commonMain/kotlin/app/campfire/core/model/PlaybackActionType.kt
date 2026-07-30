// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

/**
 * The type of playback action that was recorded for a library item.
 */
enum class PlaybackActionType {
  Play,
  Pause,
  Seek,
  SkipNext,
  SkipPrevious,
  SeekForward,
  SeekBackward,
  Sync,
}
