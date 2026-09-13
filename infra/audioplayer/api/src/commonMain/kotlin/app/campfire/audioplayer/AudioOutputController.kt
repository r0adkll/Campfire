// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer

import kotlinx.coroutines.flow.StateFlow

/**
 * Control over the app's own audio output, independent of the media session and of whatever is
 * currently playing.
 *
 * This is deliberately *not* part of [AudioPlayer]: an app-level volume is a desktop concept.
 * Android and iOS route volume through the system/media volume, so they bind an implementation
 * reporting [isSupported] false and the UI hides the control — the same shape
 * [app.campfire.audioplayer.cast.CastController] uses for a capability only some platforms have.
 *
 * Because it lives above the player rather than inside it, the control keeps working across the
 * session churn that destroys and rebuilds [AudioPlayer].
 */
interface AudioOutputController {

  /** False on platforms with no app-level volume; the UI renders no control at all. */
  val isSupported: Boolean

  /**
   * The volume slider's position, 0f..1f — the value the user set, not the gain applied to the
   * audio. Persisted.
   */
  val volume: StateFlow<Float>

  /**
   * Whether output is currently silenced. Composed with [volume] rather than replacing it, so
   * unmuting returns to the level that was already there.
   *
   * Not persisted: mute is a momentary gesture, and an app that launches silent because of
   * something the user did days ago reads as broken.
   */
  val isMuted: StateFlow<Boolean>

  /** Sets the slider position, coerced into 0f..1f. Unmutes if [volume] moves above zero. */
  fun setVolume(volume: Float)

  fun setMuted(muted: Boolean)

  fun toggleMuted() = setMuted(!isMuted.value)
}
