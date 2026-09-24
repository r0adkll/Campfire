// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.sync

/** What a sync pass learned about the server's reachability. */
internal enum class ServerContact {
  /** Nothing needed uploading, so no request was made. */
  NotAttempted,

  /** The server answered — successfully or with an error status. */
  Reached,

  /** A request got no answer at all (no route, DNS failure, timeout). */
  Unreachable,
  ;

  /** Combines two passes: any unanswered request wins, then any answered one. */
  operator fun plus(other: ServerContact): ServerContact = maxOf(this, other)
}
