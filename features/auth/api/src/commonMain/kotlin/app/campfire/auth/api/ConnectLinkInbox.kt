// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.api

import app.campfire.core.navigation.ConnectLinkResult

/**
 * Holds setup links in memory between the moment one opens Campfire and the user acting on it,
 * so the service token in a link isn't written into saved navigation state. After the process
 * dies, a link is gone and the screens that wanted it carry on without it.
 */
interface ConnectLinkInbox {

  /**
   * Hold [result] under [id], the delivering deep link's nonce. Returns `false` when [id] was
   * already delivered: the root hands the same deep link to every window it composes, but a link
   * should open once.
   */
  fun deliver(id: Long, result: ConnectLinkResult): Boolean

  /** The link held under [id], if it's still here. */
  operator fun get(id: Long): ConnectLinkResult?

  /** Forget the link held under [id], once it's been used or the user cancelled it. */
  fun discard(id: Long)
}
