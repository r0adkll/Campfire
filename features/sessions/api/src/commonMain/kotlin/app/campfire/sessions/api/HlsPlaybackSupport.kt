// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.api

/**
 * Whether this platform's player can play the server's HLS transcode streams, segment requests
 * included (they must be authenticated like the playlist). Bound per platform by the audio
 * player implementation; the streaming router consults it before offering HLS at all.
 */
interface HlsPlaybackSupport {
  val supportsHls: Boolean
}
