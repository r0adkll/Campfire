// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer

/**
 * Raised through [AudioPlayer.error] when a player can say why an item failed, so the UI can
 * name the actual problem instead of blaming the file's format for every failure.
 *
 * @param kind what went wrong, in terms the user can act on
 * @param code the platform's own name for the failure (e.g. media3's error code name), shown
 *   to the user so it can be quoted in bug reports
 */
class PlaybackFailedException(
  val kind: Kind,
  val code: String,
  message: String? = null,
  cause: Throwable? = null,
) : Exception(message ?: code, cause) {

  enum class Kind {
    /** The server couldn't be reached, or the connection dropped mid-stream. */
    Network,

    /** The server rejected the request (expired login, missing permission). */
    Unauthorized,

    /** The server no longer has the file (moved, deleted, or rescanned). */
    NotFound,

    /** The file couldn't be read: unsupported codec/container, or corrupt data. */
    UnsupportedFormat,

    /** Anything else. */
    Unknown,
  }
}
