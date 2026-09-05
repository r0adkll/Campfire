// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer

/**
 * Raised through [AudioPlayer.error] when the platform has no usable audio engine at all — on
 * desktop, when the VLC native libraries can't be found or loaded. Distinct from a per-item
 * playback failure so the UI can tell the user how to fix it rather than blaming the file.
 */
class PlaybackEngineUnavailableException(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause)
