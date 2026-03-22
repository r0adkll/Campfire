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
