// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer

/**
 * An output the app can route playback to.
 *
 * [name] is the only durable handle. Java Sound exposes nothing else — a `Mixer.Info` carries name,
 * vendor, description and version, its real device id is package-private *and* ephemeral, and its
 * `equals` is identity-based, so a held reference goes stale. The selection is therefore persisted
 * by name and re-resolved immediately before each open, never cached.
 *
 * Two identical devices (a pair of the same USB DAC) collide under that scheme. Adding vendor or
 * description would not separate them either — those fields are identical too — and an index would
 * make the dangerous case worse rather than better, so the collision is accepted as honest: the
 * user cannot tell them apart in the picker either.
 */
data class AudioDevice(
  /** Engine-specific handle. Equal to [name] on Java Sound, which has nothing better. */
  val id: String,
  /** What the picker shows, and what a selection is persisted as. */
  val name: String,
)
