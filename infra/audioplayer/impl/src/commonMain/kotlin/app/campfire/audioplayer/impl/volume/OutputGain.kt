// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.volume

/**
 * Turns the things that want to attenuate playback into the single gain an engine understands.
 *
 * There are three of them, and before this existed they all wrote
 * [app.campfire.audioplayer.impl.engine.PlaybackEngine.volume] directly. That only worked while
 * exactly one of them existed: once a user can move a slider *and* a sleep timer can fade, "the
 * gain is zero" stops being answerable — is the user muted, or is a fade finishing? The fade used
 * to capture and restore an absolute value, which clobbered a concurrent slider drag, and the
 * player used to read a zero gain as "a fade left it there" and helpfully undo it, which would
 * have silently unmuted anyone who muted deliberately.
 *
 * Keeping them as independent factors makes all of that go away: each input says only its own
 * piece, and the product is the truth.
 */
object OutputGain {

  /**
   * The linear gain for a slider at [position] (0f..1f).
   *
   * Squared rather than linear because hearing is logarithmic: a linear slider spends its top
   * half doing almost nothing and its bottom sixth doing everything. Squaring is the cheap
   * approximation of a perceptual taper — travel feels even, and it still reaches exactly 0 and 1
   * at the ends, so no floor constant has to be kept in step with the fade's own dB curve.
   */
  fun forPosition(position: Float): Float {
    val clamped = position.coerceIn(0f, 1f)
    return clamped * clamped
  }

  /**
   * The gain to hand the engine: the user's [position] on the slider, silenced by [muted], scaled
   * by [fade] — the sleep timer's ramp, 1f when no fade is running.
   */
  fun compose(position: Float, muted: Boolean, fade: Float): Float {
    if (muted) return 0f
    return forPosition(position) * fade.coerceIn(0f, 1f)
  }
}
